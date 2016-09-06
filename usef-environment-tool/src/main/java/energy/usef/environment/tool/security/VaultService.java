/*
 * Copyright 2015-2016 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package energy.usef.environment.tool.security;

import energy.usef.environment.tool.config.SortedProperties;
import energy.usef.environment.tool.config.ToolConfig;
import energy.usef.environment.tool.util.FileUtil;
import energy.usef.environment.tool.yaml.DomainConfig;
import energy.usef.environment.tool.yaml.NodeConfig;
import energy.usef.environment.tool.yaml.RoleConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.lang.RandomStringUtils;
import org.h2.tools.Server;
import org.jboss.security.plugins.PBEUtils;
import org.picketbox.plugins.vault.PicketBoxSecurityVault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which contains all methods to generate the JBoss VAULT specific files.
 */
public class VaultService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultService.class);

    private String password;
    private NodeConfig node = null;

    public VaultService(NodeConfig node) {
        this.node = node;
        String encryptionPassword = RandomStringUtils.randomAlphabetic(15);
        String connectionPassword = RandomStringUtils.randomAlphabetic(15);
        password = encryptionPassword + " " + connectionPassword;
    }

    public String getPassword() {
        return password;
    }

    private static String computeMaskedPassword(String keystorePassword) throws Exception {
        // Create the PBE secret key
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ToolConfig.VAULT_ENC_ALGORITHM);

        char[] password = "somearbitrarycrazystringthatdoesnotmatter".toCharArray();
        PBEParameterSpec cipherSpec = new PBEParameterSpec(ToolConfig.VAULT_SALT.getBytes(), ToolConfig.VAULT_ITERATION);
        PBEKeySpec keySpec = new PBEKeySpec(password);
        SecretKey cipherKey = factory.generateSecret(keySpec);

        String maskedPass = PBEUtils.encode64(keystorePassword.getBytes(), ToolConfig.VAULT_ENC_ALGORITHM, cipherKey, cipherSpec);

        return PicketBoxSecurityVault.PASS_MASK_PREFIX + maskedPass;
    }

    /**
     * Executes a class's static main method with the current java executable and classpath in a separate process.
     * 
     * @param klass the class to call the static main method for
     * @param params the parameters to provide
     * @return the exit code of the process
     * @throws IOException
     * @throws InterruptedException
     */
    public static int exec(@SuppressWarnings("rawtypes") Class klass, List<String> params) throws IOException,
            InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = klass.getCanonicalName();

        // construct the command line
        List<String> command = new ArrayList<String>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add(className);
        command.addAll(params);
        LOGGER.debug("executing class '{}' with params '{}' in classpath '{}' with java binary '{}'", className, params.toString(),
                classpath, javaBin);

        // build and start the Vault's process
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        process.waitFor();

        // get the input and error streams of the process and log them
        InputStream in = process.getInputStream();
        InputStream en = process.getErrorStream();
        InputStreamReader is = new InputStreamReader(in);
        InputStreamReader es = new InputStreamReader(en);
        BufferedReader br = new BufferedReader(is);
        BufferedReader be = new BufferedReader(es);

        String read = br.readLine();
        while (read != null) {
            LOGGER.debug(read);
            read = br.readLine();
        }
        read = be.readLine();
        while (read != null) {
            LOGGER.debug(read);
            read = be.readLine();
        }

        br.close();
        is.close();
        in.close();

        return process.exitValue();
    }

    /**
     * Creates the vault to be used by wildfly.
     * 
     * @param keystorePassword the password of the keystore
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    public void createVault(String keystorePassword) throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException {
        String keystoreFilename = ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node.getName()) + File.separator +
                ToolConfig.WILDFLY_KEYSTORE;

        // Create an empty keystore.
        KeystoreService.createNewStoreIfNeeded(keystoreFilename, keystorePassword.toCharArray());

        String[] params = { "-k" + keystoreFilename,
                "-p" + keystorePassword, "-e" + ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node.getName()),
                "-i" + ToolConfig.VAULT_ITERATION, "-s" + ToolConfig.VAULT_SALT,
                "-b" + ToolConfig.VAULT_BLOCK, "-x" + password };
        List<String> paramList = Arrays.asList(params);

        try {
            exec(org.jboss.as.security.vault.VaultTool.class, paramList);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Exception caugth while executing the jboss vault tool " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Creates a node-specific credentials.properties. All paths in the credentials.properties file are from
     * the node's point of view, so do include node(os)-specific paths and path seperators. Encrypted database(s) creation
     * is done upon first access when generating Domains
     *
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public void createEncryptedDatabase(boolean isPerParticipantDatabase) throws SQLException, ClassNotFoundException, IOException {
        LOGGER.info("Database Settings" + "\n" + "\tDriver Class {} " + "\n"
                + "\tJDBC URL {} " + "\n" + "\tUser Name: {}" + "\n"
                + "\tPassword: {}", ToolConfig.DRIVER_CLASS, ToolConfig.getUsefEnvironmentDbUrl(node.getName()), ToolConfig.USER,
                password);

        SortedProperties properties = new SortedProperties();
        properties.setProperty(ToolConfig.USEF_ROOT_FOLDER, node.getBasePath());
        properties.setProperty(ToolConfig.DB_DRIVER_PROPERTY, ToolConfig.DRIVER_CLASS);
        properties.setProperty(ToolConfig.DB_USER_PROPERTY, ToolConfig.USER);
        properties.setProperty(ToolConfig.DB_PASSWORD_PROPERTY, password);

        // Provide some backwards compatibility
        if (!isPerParticipantDatabase) {
            properties.setProperty(ToolConfig.DB_URL_PROPERTY, node.getDbUrl());
            properties.setProperty(ToolConfig.SOAP_DB_URL_PROPERTY, node.getDbUrlIncludingUsernameAndPassword(password));
        }

        for (DomainConfig domainConfig : node.getDomainConfigs()) {
            for (RoleConfig roleConfig : domainConfig.getRoleConfigs()) {
                if (isPerParticipantDatabase) {
                    properties.setProperty(ToolConfig.DB_URL_PROPERTY + "_" + roleConfig.getUniqueDbSchemaName(),  node.getDbUrl().replace("usef_db", roleConfig.getDomain()));
                    properties.setProperty(ToolConfig.SOAP_DB_URL_PROPERTY + "_" + roleConfig.getUniqueDbSchemaName(), node.getDbUrlIncludingUsernameAndPassword(password).replace("usef_db", roleConfig.getDomain()));
                } else {
                    properties.setProperty(ToolConfig.SOAP_DB_URL_PROPERTY + "_" + roleConfig.getUniqueDbSchemaName(), node.getDbUrlIncludingUsernameAndPassword(password));
                    properties.setProperty(ToolConfig.DB_URL_PROPERTY + "_" + roleConfig.getUniqueDbSchemaName(),  node.getDbUrl());
                }
            }
        }

        FileUtil.writeProperties(ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node.getName()) + File.separator +
                ToolConfig.CREDENTIALS, "Database credential properties file - REMOVE THIS FILE FOR SECURITY REASONS!", properties);
    }

    /**
     * Creates a Wildfly.properties file for the node. All paths in this file are from the node's point of view, so do include
     * node(os)-specific paths and path seperators.
     * 
     * @param keystorePassword the password of the keystore
     * @throws Exception
     */
    public void createWildflyPropertiesFile(String keystorePassword) throws Exception {
        LOGGER.info("Copying Wildfly properties.");
        String configFolder = node.getWildFlySubFolder("configuration");
        Properties properties = FileUtil.readProperties(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator +
                ToolConfig.WILDFLY_PROPERTIES);
        properties.setProperty("vault.mask", computeMaskedPassword(keystorePassword));
        properties.setProperty("jboss.server.base.dir", node.getWildFlyFolder());
        properties.setProperty("jboss.server.config.dir", configFolder);
        properties.setProperty("jboss.server.data.dir", node.getWildFlySubFolder("data"));
        properties.setProperty("jboss.server.log.dir", node.getWildFlySubFolder("log"));
        properties.setProperty("jboss.server.temp.dir", node.getWildFlySubFolder("temp"));
        properties.setProperty("jboss.server.deploy.dir", node.getWildFlySubFolder("content"));
        properties.setProperty("vault.keystore", configFolder + node.getPathSep() + ToolConfig.WILDFLY_KEYSTORE);
        properties.setProperty("vault.enc_dir", configFolder);
        properties.setProperty("jdbc.url", node.getDbUrl() + ToolConfig.TRACE_LEVEL_LOGGING);

        FileUtil.writeProperties(ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node.getName()) + File.separator +
                ToolConfig.WILDFLY_PROPERTIES, "Wildfly properties.", properties);
    }
}
