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

package energy.usef.environment.tool;

import energy.usef.environment.tool.config.SortedProperties;
import energy.usef.environment.tool.config.ToolConfig;
import energy.usef.environment.tool.security.KeystoreService;
import energy.usef.environment.tool.util.FileUtil;
import energy.usef.environment.tool.util.WarUtil;
import energy.usef.environment.tool.yaml.NodeConfig;
import energy.usef.environment.tool.yaml.Role;
import energy.usef.environment.tool.yaml.RoleConfig;
import energy.usef.environment.tool.yaml.UsefEnvironment;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.codec.binary.Base64;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Generates the global environment, like VAULT.dat, wildfly.properties etc.
 */
public class GenerateDomains {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateDomains.class);
    private static final String USEF_TIME = "usef-time";
    private static final String SERVICE_NODE_NAME = "service_node";
    private static final String PBC_FEEDER_ENDPOINT = "pbc_feeder_endpoint";
    private UsefEnvironment environmentConfig = new UsefEnvironment();



    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Generate domains from configuration usef-environment.yaml");

        LOGGER.info("Found USEF Environment folder: {}", ToolConfig.getUsefEnvironmentFolder());

        new GenerateDomains().run();
    }

    public void run() throws Exception {
        ToolConfig.checkJavaVersion();

        ToolConfig.checkBuildsOccurred();

        String environmentYaml = ToolConfig.getUsefEnvironmentConfigFolder() + File.separator +
                ToolConfig.USEF_ENVIRONMENT_YAML;
        if (!FileUtil.isFileExists(environmentYaml)) {
            LOGGER.error("Configuration file {} could not be found. Domains can not be generated.", environmentYaml);
            System.exit(2);
        }

        // when errors occur during loading the usef-environment.yaml, the application will exit 1.
        environmentConfig.load(environmentYaml);

        generateNodeFolders();
        generateDataSources();

        // copy the global yaml file to every node
        String globalParticipantFile = ToolConfig.getUsefEnvironmentNodesFolder() + File.separator +
                ToolConfig.PARTICIPANTS_YAML;
        for (String nodeName : environmentConfig.getNodeNames()) {
            String nodeParticipantFile = ToolConfig.getUsefEnvironmentDomainConfigurationFolder(nodeName) + File.separator +
                    ToolConfig.PARTICIPANTS_YAML;
            FileUtil.copyFile(globalParticipantFile, nodeParticipantFile);
        }

        // configure time server when requested
        if (!environmentConfig.getTimeServerConfig().isEmpty()) {
            generateTimeServerFolder();
        }
        if (environmentConfig.getGlobalConfig().containsKey(PBC_FEEDER_ENDPOINT)) {
            // deploy PBC feeder when it's used by participants
            copyPbcFeederWarFile();
        }

        generateH2DatabaseSchemas();
    }

    private void generateH2DatabaseSchemas() throws SQLException, IOException, ClassNotFoundException {
        Server server = Server.createTcpServer("-tcpAllowOthers").start();
        for (String nodeName : environmentConfig.getNodeNames()) {
            NodeConfig nodeConfig = environmentConfig.getNodeConfig(nodeName);

            String credentialProperties = ToolConfig.getUsefEnvironmentDomainConfigurationFolder(nodeName) + File.separator +
                    ToolConfig.CREDENTIALS;
            if (!FileUtil.isFileExists(credentialProperties)) {
                LOGGER.error("Properties file {} does not exist.", credentialProperties);
                System.exit(1);
            }

            Properties properties = FileUtil.readProperties(credentialProperties);

            Class.forName(ToolConfig.DRIVER_CLASS);

            List<RoleConfig> domains = environmentConfig.getDomainRoleConfig(nodeConfig);
            for (RoleConfig roleConfig : domains) {

                String dbFolder = ToolConfig.getUsefEnvironmentDomainDataFolder(nodeName) + File.separator;

                String dbFilename = dbFolder + (environmentConfig.isDatabasePerParticipant() ? roleConfig.getDomain() : "usef_db");

                LOGGER.info("The location of the database file: " + dbFilename);

                Connection connection = DriverManager.getConnection(ToolConfig.getUsefEnvironmentDbUrl(dbFilename), ToolConfig.USER,
                        properties.getProperty(ToolConfig.DB_PASSWORD_PROPERTY));

                List<String> statements = new ArrayList<>();

                statements.add("drop schema " + roleConfig.getUniqueDbSchemaName() + " if exists;");
                statements.add("create schema " + roleConfig.getUniqueDbSchemaName() + ";");
                statements.add("create sequence " + roleConfig.getUniqueDbSchemaName() + ".HIBERNATE_SEQUENCE;");

                List<String> ddlStatements = FileUtil.readLines(roleConfig.getDdlScript());

                for (String ddlStatement : ddlStatements) {
                    statements
                            .add(ddlStatement.replaceAll(roleConfig.getTemplateDbSchemaName(), roleConfig.getUniqueDbSchemaName())
                                    + ";");
                }

                String ddlFile = ToolConfig.getUsefEnvironmentDomainDdlFolder(nodeName) + File.separator
                        + roleConfig.getUniqueName() + File.separator + ToolConfig.DDL_FILENAME;

                BufferedOutputStream bout = null;
                try {
                    bout = new BufferedOutputStream(new FileOutputStream(ddlFile));

                    for (String line : statements) {
                        line += System.getProperty("line.separator");
                        bout.write(line.getBytes());
                    }
                } catch (IOException e) {
                } finally {
                    if (bout != null) {
                        try {
                            bout.close();
                        } catch (Exception e) {
                        }
                    }
                }

                List<String> ddlLines = FileUtil.readLines(ddlFile);

                for (String statement : ddlLines) {
                    executeDdlStatement(connection, statement);
                }

                connection.close();
            }
        }
        LOGGER.info("Closing H2 database.");
        server.stop();
    }

    private void executeDdlStatement(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            LOGGER.info("DDL: {}", sql);
            statement.executeUpdate(sql);
        }
    }

    private void generateNodeFolders() throws IOException, ParserConfigurationException, SAXException, TransformerException {
        for (String nodeName : environmentConfig.getNodeNames()) {
            NodeConfig nodeConfig = environmentConfig.getNodeConfig(nodeName);
            generateDomainFolders(nodeConfig);

        }
    }

    private void generateDataSources()  throws IOException {
        for (String nodeName : environmentConfig.getNodeNames()) {
            NodeConfig nodeConfig = environmentConfig.getNodeConfig(nodeName);

            StringBuffer datasources = new StringBuffer("");

            if (environmentConfig.isDatabasePerParticipant()) {
                List<RoleConfig> domains = environmentConfig.getDomainRoleConfig(nodeConfig);

                for (RoleConfig roleConfig : domains) {
                    String dbFilename = ToolConfig.getUsefEnvironmentDomainDataFolder(nodeName) + File.separator + roleConfig.getDomain();
                    datasources.append(getDataSource(roleConfig.getUniqueDatasourceName(), getDbUrl(dbFilename).replace("\\", "/")));
                }
            } else
            {
                String dbFilename = ToolConfig.getUsefEnvironmentDomainDataFolder(nodeName) + File.separator + "usef_db";
                datasources.append(getDataSource("USEF_DS", getDbUrl(dbFilename).replace("\\", "/")));
            }

            String standaloneXml = ToolConfig.getUsefEnvironmentDomainConfigurationFolder(nodeName) + File.separator + ToolConfig.STANDALONE_XML;

            List<String> config = new ArrayList<>();
            List<String> templateConfig = FileUtil.readLines(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator + ToolConfig.STANDALONE_XML);

            for (String line : templateConfig) {
                if ("USEF_DATASOURCES".equalsIgnoreCase(line)) {
                    config.add(datasources.toString());
                }
                else {
                    config.add(line);
                }
            }

            BufferedOutputStream bout = null;
            try {
                bout = new BufferedOutputStream(new FileOutputStream(standaloneXml));

                for (String line : config) {
                    line += System.getProperty("line.separator");
                    bout.write(line.getBytes());
                }
            } catch (IOException e) {
            } finally {
                if (bout != null) {
                    try {
                        bout.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public String getDbUrl(String file) {
        return ("jdbc:h2:tcp://127.0.0.1/" + file + ";CIPHER=AES;MVCC=true;TRACE_LEVEL_FILE=0");
    }

    private void generateDomainFolders(NodeConfig nodeConfig) throws IOException, ParserConfigurationException, SAXException,
            TransformerException {
        List<RoleConfig> domains = environmentConfig.getDomainRoleConfig(nodeConfig);
        String recipientEndpoint = environmentConfig.getGlobalConfig().get("recipient_endpoint");
        String nodeName = nodeConfig.getNode();

        for (RoleConfig roleConfig : domains) {
            String domainFolder = ToolConfig.getUsefEnvironmentDomainConfigurationFolder(nodeName) + File.separator +
                    roleConfig.getUniqueName();
            LOGGER.info("Generating domain folder {}.", domainFolder);

            if (FileUtil.isFolderExists(domainFolder)) {
                LOGGER.error("The folder {} does already exist and will NOT be recreated. Please, run the cleanup-script first!",
                        domainFolder);
                System.exit(1);
            } else {
                generateDomainFolder(nodeName, roleConfig);
                generateKeystore(nodeConfig, roleConfig, recipientEndpoint);
                generateConfigProperties(nodeName, roleConfig);
                copyAndChangeWarFile(nodeName, roleConfig);
            }
        }
        finishZoneUpdateFile();
    }

    private void generateTimeServerFolder() throws IOException, ParserConfigurationException, SAXException, TransformerException {
        Map<String, Object> timeServerSettings = environmentConfig.getTimeServerConfig();

        Map<String, String> globalConfig = environmentConfig.getGlobalConfig();
        if (!globalConfig.containsKey(SERVICE_NODE_NAME)) {
            LOGGER.error("No " + SERVICE_NODE_NAME
                    + " has been defined for the timeserver to run on, please add it to the yaml file!");
            System.exit(1);
        }
        String timeServerNode = globalConfig.get(SERVICE_NODE_NAME).toString();
        String timeServerFolder = ToolConfig.getUsefEnvironmentDomainConfigurationFolder(timeServerNode) + File.separator
                + USEF_TIME;
        LOGGER.info("Generating domain folder {}.", timeServerFolder);

        if (FileUtil.isFolderExists(timeServerFolder)) {
            LOGGER.error("The folder {} does already exist and will NOT be recreated. Please, run the cleanup-script first!",
                    timeServerFolder);
            System.exit(1);
        } else {
            FileUtil.createFolders(timeServerFolder);
            generateConfigProperties(timeServerFolder);
            // copy usef war
            String fromFilename = ToolConfig.getUsefEnvironmentBuildTimeWarFolder() + File.separator + ToolConfig.TARGET_TIME_WAR;
            String toWarFilename = ToolConfig.getUsefEnvironmentDomainDeploymentFolder(timeServerNode) + File.separator
                    + ToolConfig.TARGET_TIME_WAR;
            FileUtil.copyFile(fromFilename, toWarFilename);

            String configContent = WarUtil.retrieveContentFromWar(toWarFilename, WarUtil.USEF_TIME_CONFIG_PATH);

            Properties properties = new Properties();
            InputStream inStream = new ByteArrayInputStream(configContent.getBytes(FileUtil.DEFAULT_CHARSET));
            properties.load(inStream);

            for (Entry<String, Object> entry : timeServerSettings.entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue().toString());
            }

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            properties.store(outStream, "USEF Time Server Properties");
            String newconfigContent = new String(outStream.toByteArray(), FileUtil.DEFAULT_CHARSET);

            WarUtil.replaceFileInWar(toWarFilename, WarUtil.USEF_TIME_CONFIG, WarUtil.USEF_TIME_CONFIG_PATH, newconfigContent);
        }
    }

    private void copyPbcFeederWarFile() throws IOException, ParserConfigurationException, SAXException, TransformerException {
        Map<String, String> globalConfig = environmentConfig.getGlobalConfig();
        if (!globalConfig.containsKey(SERVICE_NODE_NAME)) {
            LOGGER.error("No " + SERVICE_NODE_NAME
                    + " has been defined for the PBC feeder to run on, please add it to the yaml file!");
            System.exit(1);
        }
        String pbcFeederNode = globalConfig.get(SERVICE_NODE_NAME).toString();
        // copy usef war
        String fromFilename = ToolConfig.getUsefEnvironmentBuildPbcFeederWarFolder() + File.separator
                + ToolConfig.TARGET_PBC_FEEDER_WAR;
        String toWarFilename = ToolConfig.getUsefEnvironmentDomainDeploymentFolder(pbcFeederNode) + File.separator
                + ToolConfig.TARGET_PBC_FEEDER_WAR;
        FileUtil.copyFile(fromFilename, toWarFilename);
    }

    private void generateConfigProperties(String domainFolder) throws IOException {
        Properties properties = new Properties();
        for (Entry<String, Object> entry : environmentConfig.getTimeServerConfig().entrySet()) {
            properties.put(entry.getKey().toUpperCase(), entry.getValue().toString());
        }
        FileUtil.writeProperties(domainFolder + File.separator + ToolConfig.LOCAL_CONFIG,
                "Properties file for USEF time server", properties);

    }

    private void copyAndChangeWarFile(String nodeName, RoleConfig roleConfig) throws IOException,
            ParserConfigurationException, SAXException, TransformerException {
        String fromFilename = roleConfig.getWarFile();
        String toFilename = ToolConfig.getUsefEnvironmentDomainDeploymentFolder(nodeName) + File.separator +
                roleConfig.getUniqueName() + ".war";
        FileUtil.copyFile(fromFilename, toFilename);

        WarUtil.replaceFilesInWar(roleConfig, toFilename);
    }

    private void generateConfigProperties(String nodeName, RoleConfig roleConfig) throws IOException {
        String domainFolder = ToolConfig.getUsefEnvironmentDomainConfigurationFolder(nodeName) + File.separator +
                roleConfig.getUniqueName();
        SortedProperties properties = new SortedProperties();

        System.out.println(
                roleConfig.getUniqueName() + (Role.AGR != roleConfig.getRole() ? " is not an aggregator " : " is an aggregator"));

        for (Entry<String, String> entry : roleConfig.getConfig().entrySet()) {
            if (entry.getKey().toUpperCase().startsWith("AGR_") && Role.AGR != roleConfig.getRole()) {
                LOGGER.info("Skipping " + entry.getKey());
                continue;
            }
            if (entry.getKey().toUpperCase().startsWith("BRP_") && Role.BRP != roleConfig.getRole()) {
                LOGGER.info("Skipping " + entry.getKey());
                continue;
            }
            if (entry.getKey().toUpperCase().startsWith("CRO_") && Role.CRO != roleConfig.getRole()) {
                LOGGER.info("Skipping " + entry.getKey());
                continue;
            }
            if (entry.getKey().toUpperCase().startsWith("DSO_") && Role.DSO != roleConfig.getRole()) {
                LOGGER.info("Skipping " + entry.getKey());
                continue;
            }
            if (entry.getKey().toUpperCase().startsWith("MDC_") && Role.MDC != roleConfig.getRole()) {
                LOGGER.info("Skipping " + entry.getKey());
                continue;
            }

            properties.put(entry.getKey().toUpperCase(), entry.getValue());
        }
        properties.put(ToolConfig.HOST_DOMAIN, roleConfig.getDomain());

        FileUtil.writeProperties(domainFolder + File.separator + ToolConfig.LOCAL_CONFIG,
                "Properties file for " + roleConfig.getUniqueName(), properties);
    }

    private void generateDomainFolder(String nodeName, RoleConfig roleConfig) throws IOException {
        String domainFolder = ToolConfig.getUsefEnvironmentDomainConfigurationFolder(nodeName) + File.separator +
                roleConfig.getUniqueName();
        FileUtil.createFolders(domainFolder);

        String ddlFolder = ToolConfig.getUsefEnvironmentDomainDdlFolder(nodeName) + File.separator +
                roleConfig.getUniqueName();
        FileUtil.createFolders(ddlFolder);

        // copy the global keystore to the domain specific folder.
        FileUtil.copyFile(ToolConfig.getUsefEnvironmentDomainConfigurationFolder(nodeName) + File.separator +
                ToolConfig.WILDFLY_KEYSTORE, domainFolder + File.separator + "role-keystore");

        // copy the pbc-catalog, except for CRO (has no pbcs)
        if (!Role.CRO.equals(roleConfig.getRole())) {
            FileUtil.copyFile(ToolConfig.getUsefEnvironmentPBCCatalogForRole(roleConfig.getRole()),
                    domainFolder + File.separator + ToolConfig.PBC_CATALOG);
        }

        // copy allow list.
        FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator +
                ToolConfig.ALLOW_LIST, domainFolder + File.separator + ToolConfig.ALLOW_LIST);

        // copy deny list.
        FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator +
                ToolConfig.DENY_LIST, domainFolder + File.separator + ToolConfig.DENY_LIST);

        // copy LogBackWithAdditionalLoggers.xml.
        FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator +
                ToolConfig.LOGBACK, domainFolder + File.separator + ToolConfig.LOGBACK);

        // copy capabilities.json for AGR
        if (Role.AGR.equals(roleConfig.getRole())) {
            FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator +
                    ToolConfig.CAPABILITIES_JSON, domainFolder + File.separator + ToolConfig.CAPABILITIES_JSON);
        }
    }

    private void generateKeystore(NodeConfig nodeConfig, RoleConfig roleConfig, String recipientEndpoint) throws IOException {
        String nodeName = nodeConfig.getNode();
        String domainFolder = ToolConfig.getUsefEnvironmentDomainConfigurationFolder(nodeName) + File.separator +
                roleConfig.getUniqueName();
        String keystoreFilename = domainFolder + File.separator + ToolConfig.LOCAL_KEYSTORE;

        KeystoreService keystoreService = new KeystoreService(
                keystoreFilename,
                environmentConfig.getKeystorePassword(),
                ToolConfig.KEYSTORE_PRIVATE_KEY_ALIAS,
                environmentConfig.getKeyStorePrivateKeyPassword());

        LOGGER.debug("Creating a new NaCl secret key in the keystore.");
        byte[] publicKey = keystoreService.createSecretKey(ToolConfig.KEYSTORE_SEED);

        String base64PublicKey = Base64.encodeBase64String(publicKey);
        LOGGER.info("Generated base64 public key: {}", base64PublicKey);

        // adding public key to the participantYaml file.
        addPublicKeyToParticipantYaml(roleConfig, base64PublicKey, recipientEndpoint);

        addParticipantToZoneUpdateFile(nodeConfig, roleConfig, base64PublicKey);
    }

    private void addPublicKeyToParticipantYaml(RoleConfig roleConfig, String publicKey, String recipientEndpoint)
            throws IOException {
        String participantFilename = ToolConfig.getUsefEnvironmentNodesFolder() + File.separator +
                ToolConfig.PARTICIPANTS_YAML;

        if (!FileUtil.isFileExists(participantFilename)) {
            LOGGER.error("Could not find file {}.", participantFilename);
            System.exit(1);
        }

        String domainName = roleConfig.getDomain();

        StringBuilder sb = new StringBuilder();
        sb.append("    - domain-name: \"" + domainName + "\"\n");
        sb.append("      spec-version: \"2015\"\n");
        sb.append("      " + roleConfig.getRole().getRoleNameInParticipantsYaml() + ":\n");
        sb.append("        - public-keys: \"" + ToolConfig.PUBLIC_KEY_PREFIX + publicKey + "\"\n");
        sb.append("          url: \"https://" + domainName);
        /*
         * If USEF-compliant URL's are used, recipientEndpoint is null. Then a proxy server needs to be used to translate the URL to
         * a form suitable for Wildfly. Otherwise recipientEndpoint specifies the last part of the URL which can be used directly in
         * Wildfly.
         */
        if (recipientEndpoint == null) {
            sb.append("/USEF/2015/SignedMessage\"\n");
        } else {
            sb.append(":8443/" + roleConfig.getUniqueName() + recipientEndpoint + "\"\n");
        }

        FileUtil.appendTextToFile(participantFilename, sb.toString());
    }

    private void addParticipantToZoneUpdateFile(NodeConfig nodeConfig, RoleConfig roleConfig, String publicKey)
            throws IOException {
        String zoneFilename = ToolConfig.getUsefEnvironmentNodesFolder() + File.separator +
                ToolConfig.BIND_ZONE_FILE;
        final String usefprefix = "_usef";
        final String httpprefix = "_http";
        final String specVersion = "2015";
        final String ttl = " 86400 ";
        final String delete = "update delete ";
        final String add = "update add ";

        if (!FileUtil.isFileExists(zoneFilename)) {
            LOGGER.error("Could not find file {}.", zoneFilename);
            System.exit(1);
        }

        String roleName = roleConfig.getRole().name().toLowerCase();
        String address = nodeConfig.getAddress();
        String domainName = roleConfig.getDomain();
        String specSubDomain = usefprefix + "." + domainName;
        String keysSubDomain = "_" + roleName + "." + specSubDomain;
        String httpSubDomain = httpprefix + "." + keysSubDomain;

        StringBuilder sb = new StringBuilder();

        sb.append(delete + domainName + "\n");
        sb.append(add + domainName + ttl + "A " + address + "\n");
        sb.append(delete + specSubDomain + "\n");
        sb.append(add + specSubDomain + ttl + "IN TXT \"" + specVersion + "\"\n");
        sb.append(delete + keysSubDomain + "\n");
        sb.append(add + keysSubDomain + ttl + "IN TXT \"" + ToolConfig.PUBLIC_KEY_PREFIX + publicKey + "\"\n");
        sb.append(delete + httpSubDomain + "\n");
        sb.append(add + httpSubDomain + ttl + "IN CNAME " + domainName + "\n");

        /*
         * If USEF-compliant URL's are used, recipientEndpoint is null. Then a proxy server needs to be used to translate the URL to
         * a form suitable for Wildfly. Otherwise recipientEndpoint specifies the last part of the URL which can be used directly in
         * Wildfly.
         */

        FileUtil.appendNonDelimitingTextToFile(zoneFilename, sb.toString());
    }

    private void finishZoneUpdateFile() throws IOException {
        String zoneFilename = ToolConfig.getUsefEnvironmentNodesFolder() + File.separator +
                ToolConfig.BIND_ZONE_FILE;
        StringBuilder sb = new StringBuilder();

        sb.append("show\n");
        sb.append("send\n");
        FileUtil.appendNonDelimitingTextToFile(zoneFilename, sb.toString());
    }


    private String getDataSource (String datasourceName, String url) {
        StringBuffer sb=new StringBuffer("");
        sb.append("                <xa-datasource jndi-name=\"java:jboss/datasources/");
        sb.append(datasourceName);
        sb.append("\" pool-name=\"");
        sb.append(datasourceName+"_POOL");
        sb.append("\" enabled=\"true\">\n");
        sb.append("                    <xa-datasource-property name=\"URL\">");
        sb.append(url);
        sb.append("</xa-datasource-property>\n");
        sb.append("                    <driver>h2</driver>\n");
        sb.append("                    <xa-pool>\n");
        sb.append("                        <min-pool-size>10</min-pool-size>\n");
        sb.append("                        <max-pool-size>20</max-pool-size>\n");
        sb.append("                        <prefill>true</prefill>\n");
        sb.append("                    </xa-pool>\n");
        sb.append("                    <security>\n");
        sb.append("                        <user-name>usef</user-name>\n");
        sb.append("                        <password>${VAULT::USEF_DS_ENCRYPTED::password::1}</password>\n");
        sb.append("                    </security>\n");
        sb.append("                </xa-datasource>\n");

        return sb.toString();
    }


}
