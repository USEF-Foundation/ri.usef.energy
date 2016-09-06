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

package energy.usef.environment.tool.config;

import energy.usef.environment.tool.util.FileUtil;
import energy.usef.environment.tool.yaml.Role;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolConfig.class);
    private static final String USEF_ENVIRONMENT_FOLDER = "usef-environment";

    // Target WAR constants
    public static final String TARGET_TIME_WAR = "usef-time.war";
    public static final String TARGET_PBC_FEEDER_WAR = "usef-pbcfeeder.war";
    public static final String TARGET_AGR_WAR = "agr.usef-example.com_AGR.war";
    public static final String TARGET_BRP_WAR = "brp.usef-example.com_BRP.war";
    public static final String TARGET_CRO_WAR = "cro.usef-example.com_CRO.war";
    public static final String TARGET_DSO_WAR = "dso.usef-example.com_DSO.war";
    public static final String TARGET_MDC_WAR = "mdc.usef-example.com_MDC.war";

    public static final String DDL_FILENAME = "create-script.sql";

    // Keystore constants
    public static final String KEYSTORE_SEED = "12345678";
    public static final String KEYSTORE_PRIVATE_KEY_ALIAS = "usef";
    public static final String PUBLIC_KEY_PREFIX = "cs1.";

    // File constants
    public static final String USEF_ENVIRONMENT_YAML = "usef-environment.yaml";
    public static final String PARTICIPANTS_YAML = "participants_dns_info.yaml";
    public static final String BIND_ZONE_FILE = "usef_bind.nsupdate";
    public static final String STANDALONE_XML = "standalone-usef.xml";
    public static final String KEYSTORE_CONFIG = "keystore-config.properties";
    public static final String SSL_KEYSTORE = "usef.jks";
    public static final String WILDFLY_KEYSTORE = "wildfly-keystore";
    public static final String WILDFLY_PROPERTIES = "wildfly.properties";
    public static final String DB_FILE = "usef_db";
    public static final String DB_FILE_EXTENSION = "h2.db";
    public static final String ALLOW_LIST = "transport-allowlist.yaml";
    public static final String DENY_LIST = "transport-denylist.yaml";
    public static final String CAPABILITIES_JSON = "capabilities.json";
    public static final String PBC_CATALOG = "pbc-catalog.properties";
    public static final String LOCAL_CONFIG = "config-local.properties";
    public static final String LOCAL_KEYSTORE = "role-keystore";
    public static final String LOGBACK = "LogBackWithAdditionalLoggers.xml";
    public static final String CREDENTIALS = "credentials.properties";
    public static final String TIME_CONFIG = "time-config.properties";

    public static final String APPLICATION_ROLES = "application-roles.properties";
    public static final String APPLICATION_USERS = "application-users.properties";
    public static final String MGMT_GROUPS = "mgmt-groups.properties";
    public static final String MGMT_USERS = "mgmt-users.properties";
    public static final String LOGGING_PROP = "logging.properties";

    public static final String PBC_FEEDER_INPUT_DATA = "stubinputdata.xls";

    // VAULT constants
    public static final String VAULT_SALT = "1245FBSD";
    public static final int VAULT_ITERATION = 25;
    public static final String VAULT_ALIAS = "vault";
    public static final String VAULT_BLOCK = "USEF_DS_ENCRYPTED";
    public static final String VAULT_ATTRIBUTE = "password";
    public static final String VAULT_ENC_ALGORITHM = "PBEwithMD5andDES";

    // Database constants
    public static final String USER = "usef";
    public static final String DRIVER_CLASS = "org.h2.Driver";
    public static final String DB_PASSWORD_PROPERTY = "DB_PASSWORD";
    public static final String DB_URL_PROPERTY = "DB_URL";
    public static final String DB_DRIVER_PROPERTY = "DB_DRIVER";
    public static final String DB_USER_PROPERTY = "DB_USER";
    public static final String TRACE_LEVEL_LOGGING = ";TRACE_LEVEL_FILE=0";

    public static final String USEF_ROOT_FOLDER = "USEF_ROOT_FOLDER";

    // Soap UI constants
    public static final String SOAP_DB_URL_PROPERTY = "SOAP_DB_URL_PROPERTY";

    // Role host domain
    public static final String HOST_DOMAIN = "HOST_DOMAIN";

    private ToolConfig() {
        // empty constructor.
    }

    public static String getUsefRootFolder() {
        String usefRootFolder = null;
        String currentDirectory = FileUtil.getCurrentFolder();
        if (currentDirectory.endsWith("usef-environment-tool")) {
            usefRootFolder = currentDirectory + File.separator + "..";
        } else {
            if (currentDirectory.endsWith("target")) {
                usefRootFolder = currentDirectory + File.separator + ".." + File.separator + "..";
            }
        }
        usefRootFolder = FilenameUtils.normalize(usefRootFolder);
        if (!FileUtil.isFolderExists(usefRootFolder)) {
            String error = "Trying to locate USEF environment folder and could not find the folder: " + usefRootFolder;
            LOGGER.error(error);
            throw new RuntimeException(error);
        }
        return usefRootFolder;
    }

    public static String getUsefEnvironmentFolder() {
        String usefEnvironmentFolder = null;
        String currentDirectory = FileUtil.getCurrentFolder();
        if (currentDirectory.endsWith("usef-environment-tool")) {
            usefEnvironmentFolder = currentDirectory + File.separator + ".." + File.separator + USEF_ENVIRONMENT_FOLDER;
        } else {
            if (currentDirectory.endsWith("target")) {
                usefEnvironmentFolder = currentDirectory + File.separator + ".." + File.separator
                        + ".." + File.separator + USEF_ENVIRONMENT_FOLDER;
            }
        }
        usefEnvironmentFolder = FilenameUtils.normalize(usefEnvironmentFolder);
        if (!FileUtil.isFolderExists(usefEnvironmentFolder)) {
            String error = "Trying to locate USEF environment folder and could not find the folder: " + usefEnvironmentFolder;
            LOGGER.error(error);
            throw new RuntimeException(error);
        }
        return usefEnvironmentFolder;
    }

    public static String getUsefEnvironmentPBCCatalogForRole(Role role) {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-simulation" + File.separator + "usef-simulation-" + role.name().toLowerCase() +
                File.separator + "src" + File.separator + "main" +File.separator + "resources"  + File.separator + ToolConfig.PBC_CATALOG  ;
    }

    public static String getUsefEnvironmentBuildTimeWarFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-time" +
                File.separator + "target";
    }

    public static String getUsefEnvironmentBuildPbcFeederWarFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-pbcfeeder" +
                File.separator + "target";
    }

    public static String getUsefEnvironmentBuildAgrWarFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-agr" +
                File.separator + "target";
    }

    public static String getUsefEnvironmentBuildBrpWarFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-brp" +
                File.separator + "target";
    }

    public static String getUsefEnvironmentBuildCroWarFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-cro" +
                File.separator + "target";
    }

    public static String getUsefEnvironmentBuildDsoWarFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-dso" +
                File.separator + "target";
    }

    public static String getUsefEnvironmentBuildMdcWarFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-mdc" +
                File.separator + "target";
    }

    public static String getUsefEnvironmentBuildAgrDdlFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-agr" +
                File.separator + "target" + File.separator + "ddl";
    }

    public static String getUsefEnvironmentBuildCroDdlFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-cro" +
                File.separator + "target" + File.separator + "ddl";
    }

    public static String getUsefEnvironmentBuildDsoDdlFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-dso" +
                File.separator + "target" + File.separator + "ddl";
    }

    public static String getUsefEnvironmentBuildMdcDdlFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-mdc" +
                File.separator + "target" + File.separator + "ddl";
    }

    public static String getUsefEnvironmentBuildBrpDdlFolder() {
        return getUsefEnvironmentBuildFolder() + File.separator + "usef-deployments" + File.separator + "usef-deployment-brp" +
                File.separator + "target" + File.separator + "ddl";
    }

    public static String getUsefEnvironmentBuildFolder() {
        return getUsefEnvironmentFolder() + File.separator + ".." + File.separator + "usef-build";
    }

    public static String getUsefEnvironmentConfigFolder() {
        return getUsefEnvironmentFolder() + File.separator + "config";
    }

    public static String getUsefEnvironmentTemplateFolder() {
        return getUsefEnvironmentFolder() + File.separator + "template";
    }

    public static String getUsefEnvironmentNodesFolder() {
        return getUsefEnvironmentFolder() + File.separator + "nodes";
    }

    public static String getUsefEnvironmentNodeFolder(String nodeName) {
        return getUsefEnvironmentNodesFolder() + File.separator + nodeName;
    }

    public static String getUsefEnvironmentDomainConfigurationFolder(String nodeName) {
        return getUsefEnvironmentNodeFolder(nodeName) + File.separator + "configuration";
    }

    public static String getUsefEnvironmentDomainDataFolder(String nodeName) {
        return getUsefEnvironmentNodeFolder(nodeName) + File.separator + "data";
    }

    public static String getUsefEnvironmentDomainDeploymentFolder(String nodeName) {
        return getUsefEnvironmentNodeFolder(nodeName) + File.separator + "deployments";
    }

    public static String getUsefEnvironmentDomainDdlFolder(String nodeName) {
        return getUsefEnvironmentNodeFolder(nodeName) + File.separator + "ddl";
    }

    public static String getUsefEnvironmentDomainLogFolder(String nodeName) {
        return getUsefEnvironmentNodeFolder(nodeName) + File.separator + "log";
    }

    public static String getUsefEnvironmentDomainTempFolder(String nodeName) {
        return getUsefEnvironmentNodeFolder(nodeName) + File.separator + "temp";
    }

    public static String getUsefEnvironmentDomainContentFolder(String nodeName) {
        return getUsefEnvironmentNodeFolder(nodeName) + File.separator + "content";
    }

    public static String getUsefEnvironmentDomainLibFolder1(String nodeName) {
        return getUsefEnvironmentNodeFolder(nodeName) + File.separator + "lib";
    }

    public static String getUsefEnvironmentDbUrl(String filename) {

        String url = "jdbc:h2:tcp://localhost/" + filename.replace(File.separatorChar, '/') + ";CIPHER=AES;MVCC=true";
        return url;
    }

    public static String getUsefEnvironmentDbUrlInclusiveUsernameAndPassword(String nodeName, String password) {
        return getUsefEnvironmentDbUrl(nodeName) + ";USER=" + ToolConfig.USER + ";PASSWORD=" + password;
    }

    public static String getTempFolder() {
        return System.getProperty("java.io.tmpdir");
    }

    public static void checkBuildsOccurred() {
        String agrWarFilename = ToolConfig.getUsefEnvironmentBuildAgrWarFolder() + File.separator + ToolConfig.TARGET_AGR_WAR;
        if (!FileUtil.isFileExists(agrWarFilename)) {
            LOGGER.error("Can not find file {}. Probably, the build of the AGR component has not succeeded.",
                    agrWarFilename);
            System.exit(1);
        }

        String brpWarFilename = ToolConfig.getUsefEnvironmentBuildBrpWarFolder() + File.separator + ToolConfig.TARGET_BRP_WAR;
        if (!FileUtil.isFileExists(brpWarFilename)) {
            LOGGER.error("Can not find file {}. Probably, the build of the BRP component has not succeeded.",
                    brpWarFilename);
            System.exit(1);
        }

        String croWarFilename = ToolConfig.getUsefEnvironmentBuildCroWarFolder() + File.separator + ToolConfig.TARGET_CRO_WAR;
        if (!FileUtil.isFileExists(croWarFilename)) {
            LOGGER.error("Can not find file {}. Probably, the build of the CRO component has not succeeded.",
                    croWarFilename);
            System.exit(1);
        }

        String dsoWarFilename = ToolConfig.getUsefEnvironmentBuildDsoWarFolder() + File.separator + ToolConfig.TARGET_DSO_WAR;
        if (!FileUtil.isFileExists(dsoWarFilename)) {
            LOGGER.error("Can not find file {}. Probably, the build of the DSO component has not succeeded.",
                    dsoWarFilename);
            System.exit(1);
        }

        String mdcWarFilename = ToolConfig.getUsefEnvironmentBuildMdcWarFolder() + File.separator + ToolConfig.TARGET_MDC_WAR;
        if (!FileUtil.isFileExists(mdcWarFilename)) {
            LOGGER.error("Can not find file {}. Probably, the build of the MDC component has not succeeded.",
                    mdcWarFilename);
            System.exit(1);
        }
    }

    public static void checkIfDbDoesNotExist(String nodeName) {
        String filename = getUsefEnvironmentDomainDataFolder(nodeName) + File.separator + DB_FILE + "." + DB_FILE_EXTENSION;
        if (FileUtil.isFileExists(filename)) {
            LOGGER.error("The database should not exist in this stage of the script. Please, run the cleanup script.\n" +
                    "The location of the database: {}", filename);
            System.exit(1);
        }
    }

    public static void checkJavaVersion() {
        String bit64 = System.getProperty("java.vm.name"); // should contain 64.
        String specVersion = System.getProperty("java.vm.specification.version"); // 1.8
        if (bit64 == null || bit64.isEmpty()) {
            LOGGER.warn("Could not find the java.vm.name environment variabele.");
        } else if (specVersion == null || specVersion.isEmpty()) {
            LOGGER.warn("Could not find the java.vm.specification.version environment variabele.");
        } else {
            Float version = Float.parseFloat(specVersion);
            if (version != 0d && version < 1.8f) {
                LOGGER.error("The software should run at least on version 1.8 of Java JDK 64-bits. The current version is {}",
                        version);
                System.exit(1);
            }
            if (!bit64.contains("64")) {
                LOGGER.error("The software should run at least on version 1.8 of Java JDK 64-bits. The environment variabele " +
                        "java.vm.name contains {} and should contain the value 64.", bit64);
                System.exit(1);
            }
        }
    }
}
