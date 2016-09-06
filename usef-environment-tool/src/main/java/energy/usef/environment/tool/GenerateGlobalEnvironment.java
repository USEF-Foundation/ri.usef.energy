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

import energy.usef.environment.tool.config.ToolConfig;
import energy.usef.environment.tool.security.VaultService;
import energy.usef.environment.tool.util.FileUtil;
import energy.usef.environment.tool.yaml.UsefEnvironment;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates the global environment, like VAULT.dat, wildfly.properties etc.
 */
public class GenerateGlobalEnvironment {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateGlobalEnvironment.class);
    private UsefEnvironment environmentConfig = new UsefEnvironment();

    public void run() throws Exception {
        ToolConfig.checkJavaVersion();

        ToolConfig.checkBuildsOccurred();

        String environmentYaml = ToolConfig.getUsefEnvironmentConfigFolder() + File.separator + ToolConfig.USEF_ENVIRONMENT_YAML;

        if (!FileUtil.isFileExists(environmentYaml)) {
            LOGGER.error("Configuration file {} could not be found. Domains can not be generated.", environmentYaml);
            System.exit(2);
        }

        // when errors occur during loading the usef-environment.yaml, the application will exit 1.
        environmentConfig.load(environmentYaml);

        String keystorePassword = environmentConfig.getKeystorePassword();

        LOGGER.debug("preparing globally for nodes {}", environmentConfig.getNodeNames().toString());
        for (String node : environmentConfig.getNodeNames()) {
            ToolConfig.checkIfDbDoesNotExist(node);
            createFolders(node);
            copyFiles(node);
            VaultService vaultService = new VaultService(environmentConfig.getNodeConfig(node));
            vaultService.createWildflyPropertiesFile(keystorePassword);
            vaultService.createEncryptedDatabase(environmentConfig.isDatabasePerParticipant());
            vaultService.createVault(keystorePassword);
        }

        String participantYamlSource = ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator
                + ToolConfig.PARTICIPANTS_YAML;
        String participantYamlTarget = ToolConfig.getUsefEnvironmentNodesFolder() + File.separator + ToolConfig.PARTICIPANTS_YAML;
        FileUtil.copyFile(participantYamlSource, participantYamlTarget);

        String zoneFileSource = ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator + ToolConfig.BIND_ZONE_FILE;
        String zoneFileTarget = ToolConfig.getUsefEnvironmentNodesFolder() + File.separator + ToolConfig.BIND_ZONE_FILE;
        FileUtil.copyFile(zoneFileSource, zoneFileTarget);

    }

    private void createFolders(String node) {
        FileUtil.createFolders(ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node));
        FileUtil.createFolders(ToolConfig.getUsefEnvironmentDomainDataFolder(node));
        FileUtil.createFolders(ToolConfig.getUsefEnvironmentDomainDeploymentFolder(node));
    }

    private void copyFiles(String node) throws IOException {
//        FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator + ToolConfig.STANDALONE_XML,
//                ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node) + File.separator + ToolConfig.STANDALONE_XML);

        FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator + ToolConfig.SSL_KEYSTORE,
                ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node) + File.separator + ToolConfig.SSL_KEYSTORE);

        // copy Wildfly specific files for users and groups.
        FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator +
                ToolConfig.APPLICATION_ROLES, ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node) + File.separator +
                ToolConfig.APPLICATION_ROLES);
        FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator +
                ToolConfig.APPLICATION_USERS, ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node) + File.separator +
                ToolConfig.APPLICATION_USERS);
        FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator +
                ToolConfig.MGMT_GROUPS, ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node) + File.separator +
                ToolConfig.MGMT_GROUPS);
        FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator +
                ToolConfig.MGMT_USERS, ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node) + File.separator +
                ToolConfig.MGMT_USERS);
        FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator +
                ToolConfig.LOGGING_PROP, ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node) + File.separator +
                ToolConfig.LOGGING_PROP);

        // copy Stubinputdata excel sheet from the template to the node folder
        FileUtil.copyFile(ToolConfig.getUsefEnvironmentTemplateFolder() + File.separator + ToolConfig.PBC_FEEDER_INPUT_DATA,
                ToolConfig.getUsefEnvironmentDomainConfigurationFolder(node) + File.separator + ToolConfig.PBC_FEEDER_INPUT_DATA);
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Generate global environment.");

        LOGGER.info("Found USEF Environment folder: {}", ToolConfig.getUsefEnvironmentFolder());

        new GenerateGlobalEnvironment().run();
    }

}
