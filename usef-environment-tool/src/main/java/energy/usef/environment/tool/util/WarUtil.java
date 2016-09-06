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

package energy.usef.environment.tool.util;

import energy.usef.environment.tool.config.PropertiesUtil;
import energy.usef.environment.tool.config.SqlScriptUtil;
import energy.usef.environment.tool.config.ToolConfig;
import energy.usef.environment.tool.config.XmlConfig;
import energy.usef.environment.tool.yaml.RoleConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class WarUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(WarUtil.class);

    private static final String SLASH = "/";

    public static final String CLASSES = "classes";
    public static final String WEB_INF = "WEB-INF";
    public static final String META_INF = "META-INF";

    public static final String JBOSS_EJB3_XML = "jboss-ejb3.xml";
    public static final String JBOSS_WEB_XML = "jboss-web.xml";
    public static final String QUEUE_CONFIG_JMS = "queue-config-jms.xml";
    public static final String PERSISTENCE_XML = "persistence.xml";
    public static final String LOGBACK_XML = "logback.xml";
    public static final String APPLICATION_PROP = "application.properties";
    public static final String USEF_TIME_CONFIG = "time-config.properties";

    public static final String CREATE_SCRIPT = "create-script.sql";
    public static final String DROP_SCRIPT = "drop-script.sql";
    public static final String LOAD_SCRIPT = "load-script.sql";

    public static final String USEF_TIME_CONFIG_PATH = WEB_INF + SLASH + CLASSES + SLASH + USEF_TIME_CONFIG;

    public static final String JBOSS_EJB3_XML_PATH = WEB_INF + SLASH + JBOSS_EJB3_XML;
    public static final String JBOSS_WEB_XML_PATH = WEB_INF + SLASH + JBOSS_WEB_XML;
    public static final String QUEUE_CONFIG_JMS_PATH = WEB_INF + SLASH + QUEUE_CONFIG_JMS;
    public static final String PERSISTENCE_XML_PATH = WEB_INF + SLASH + CLASSES + SLASH + META_INF + SLASH + PERSISTENCE_XML;
    public static final String LOGBACK_XML_PATH = WEB_INF + SLASH + CLASSES + SLASH + LOGBACK_XML;
    public static final String APPLICATION_PROP_PATH = WEB_INF + SLASH + CLASSES + SLASH + APPLICATION_PROP;

    public static final String CREATE_SCRIPT_PATH = WEB_INF + SLASH + CLASSES + SLASH + META_INF + SLASH + CREATE_SCRIPT;
    public static final String DROP_SCRIPT_PATH = WEB_INF + SLASH + CLASSES + SLASH + META_INF + SLASH + DROP_SCRIPT;
    public static final String LOAD_SCRIPT_PATH = WEB_INF + SLASH + CLASSES + SLASH + META_INF + SLASH + LOAD_SCRIPT;

    private WarUtil() {
        // private constructor.
    }

    public static void replaceFilesInWar(RoleConfig roleConfig, String warFilename) throws IOException,
            ParserConfigurationException, SAXException, TransformerException {

        replaceConfigXmlFileInWar(roleConfig, warFilename, JBOSS_EJB3_XML_PATH, JBOSS_EJB3_XML);
        replaceConfigXmlFileInWar(roleConfig, warFilename, JBOSS_WEB_XML_PATH, JBOSS_WEB_XML);
        replaceConfigXmlFileInWar(roleConfig, warFilename, QUEUE_CONFIG_JMS_PATH, QUEUE_CONFIG_JMS);
        replaceConfigXmlFileInWar(roleConfig, warFilename, PERSISTENCE_XML_PATH, PERSISTENCE_XML);
        replaceConfigXmlFileInWar(roleConfig, warFilename, LOGBACK_XML_PATH, LOGBACK_XML);

        replaceConfigXmlFileInWar(roleConfig, warFilename, CREATE_SCRIPT_PATH, CREATE_SCRIPT);
        replaceConfigXmlFileInWar(roleConfig, warFilename, DROP_SCRIPT_PATH, DROP_SCRIPT);
        replaceConfigXmlFileInWar(roleConfig, warFilename, LOAD_SCRIPT_PATH, LOAD_SCRIPT);
        replaceConfigXmlFileInWar(roleConfig, warFilename, APPLICATION_PROP_PATH, APPLICATION_PROP);
    }

    private static void replaceConfigXmlFileInWar(RoleConfig roleConfig, String warFilename,
            String pathAndNameOfFileToReplaceInWar,
            String filenameOfConfigXml) throws IOException,
            ParserConfigurationException, SAXException, TransformerException {
        LOGGER.info("In WAR {} the file {} will be replaced.", warFilename, pathAndNameOfFileToReplaceInWar);
        String xmlOrSql = WarUtil.retrieveContentFromWar(warFilename, pathAndNameOfFileToReplaceInWar);
        if (xmlOrSql == null || xmlOrSql.trim().isEmpty()) {
            LOGGER.warn("Could not find in the WAR {} the file {}.", warFilename, pathAndNameOfFileToReplaceInWar);
        } else {
            String newXmlOrSql = null;
            switch (filenameOfConfigXml) {
            case JBOSS_EJB3_XML:
                newXmlOrSql = XmlConfig.processEjb3Xml(xmlOrSql, roleConfig.getUniqueName());
                break;
            case JBOSS_WEB_XML:
                newXmlOrSql = XmlConfig.processWebXml(xmlOrSql, roleConfig.getUniqueName());
                break;
            case QUEUE_CONFIG_JMS:
                newXmlOrSql = XmlConfig.processQueueConfigJms(xmlOrSql, roleConfig.getUniqueName());
                break;
            case PERSISTENCE_XML:
                newXmlOrSql = XmlConfig.processPersistenceXml(xmlOrSql, roleConfig);
                break;
            case LOGBACK_XML:
                newXmlOrSql = XmlConfig.processLogbackXml(xmlOrSql, roleConfig.getUniqueName());
                break;
            case CREATE_SCRIPT:
                newXmlOrSql = SqlScriptUtil.processSQLScript(roleConfig.getRole(), xmlOrSql, roleConfig.getUniqueDbSchemaName());
                break;
            case DROP_SCRIPT:
                newXmlOrSql = SqlScriptUtil.processSQLScript(roleConfig.getRole(), xmlOrSql, roleConfig.getUniqueDbSchemaName());
                break;
            case LOAD_SCRIPT:
                newXmlOrSql = SqlScriptUtil.processSQLScript(roleConfig.getRole(), xmlOrSql, roleConfig.getUniqueDbSchemaName());
                break;
            case APPLICATION_PROP:
                newXmlOrSql = PropertiesUtil.process(xmlOrSql, roleConfig.getUniqueName());
                break;

            default:
                LOGGER.error("Unknown configuration file: {}\nThe USEF Environment Tooling needs to be adjusted.",
                        filenameOfConfigXml);
                System.exit(1);
            }

            WarUtil.replaceFileInWar(warFilename, filenameOfConfigXml, pathAndNameOfFileToReplaceInWar, newXmlOrSql);
        }
    }

    public static void replaceFileInWar(String warFilename, String filenameOfXml, String pathAndNameOfFileToReplaceInWar,
            String changesContentXml) throws IOException {
        if (ToolConfig.getTempFolder() == null || ToolConfig.getTempFolder().trim().isEmpty()) {
            LOGGER.error("Can not find temp folder to store files. Place -Djava.io.tmpdir=C:\temp when calling Java to solve " +
                    "this issue.");
            System.exit(1);
        }

        String tempFile = ToolConfig.getTempFolder() + File.separator + filenameOfXml;
        FileUtil.writeTextToFile(tempFile, changesContentXml);
        Path myFilePath = Paths.get(tempFile);

        Path zipFilePath = Paths.get(warFilename);
        FileSystem fs;
        fs = FileSystems.newFileSystem(zipFilePath, null);

        Path fileInsideZipPath = fs.getPath(File.separator + pathAndNameOfFileToReplaceInWar);
        Files.copy(myFilePath, fileInsideZipPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        fs.close();

        FileUtil.removeFile(tempFile, false);
    }

    public static String retrieveContentFromWar(String warFilename, String filenameToFind) throws IOException {
        try (ZipFile zipFile = new ZipFile(warFilename)) {
            Enumeration<? extends ZipEntry> e = zipFile.entries();

            while (e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
                String entryName = entry.getName();
                if (entryName.equalsIgnoreCase(filenameToFind)) {

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(zipFile.getInputStream(entry)));
                    StringWriter sw = new StringWriter();
                    char[] buffer = new char[1024 * 4];
                    int n = 0;
                    while (-1 != (n = br.read(buffer))) {
                        sw.write(buffer, 0, n);
                    }
                    return sw.toString();
                }
            }
        }
        return null;
    }

}
