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

package energy.usef.time;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class Config {

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    public static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String APPLICATION_NAME_PROPERTY = "APPLICATION_NAME";
    protected static final String CONFIG_LOCAL_FILE_NAME = "config-local.properties";

    public static final String CONFIG_FILE_NAME = "config.properties";
    public static final String CONFIG_FOLDER_PROPERTY = "jboss.server.config.dir";
    public static final String DOMAIN_CONFIG_FOLDER = System.getProperty(CONFIG_FOLDER_PROPERTY);
    private static final String SRC_TEST_RESOURCES_FOLDER = "src/test/resources/";
    protected Properties properties = new Properties();

    /*
     * Initialize a bean after the instance has been constructed.
     */

    public Config() {
        try {
            readProperties();
        } catch (IOException e) {
            LOGGER.error("Error while loading the properties: {}.", e.getMessage(), e);
        }
    }

    public Properties getProperties() {
        return properties;
    }

    /***
     * Reads the configuration properties which consist of application specific properties which overrule default properties.
     *
     * @throws IOException
     */
    public void readProperties() throws IOException {
        properties.clear();
        String filename = getConfigurationFolder() + CONFIG_LOCAL_FILE_NAME;
        Properties defaults = readDefaultProperties();
        if (isFileExists(filename)) {
            properties = readPropertiesFromFile(filename);

            // merge default properties if the property does not exist.
            for (Map.Entry<Object, Object> entry : defaults.entrySet()) {
                String key = (String) entry.getKey();
                if (properties.getProperty(key) == null) {
                    LOGGER.debug("Default property is added: {}", key);
                    properties.put(key, entry.getValue());
                }
            }

        } else {
            LOGGER.warn("Could not find properties file: {}. Using the default properties.", filename);
            properties = defaults;
        }

        List<String> propertiesList = properties.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.toList());
        Collections.sort(propertiesList);

        LOGGER.info("\nProperties:\n" + StringUtils.join(propertiesList.toArray(), "\n"));
    }

    /***
     * Checks if the file exist.
     *
     * @param fileName the name of the file.
     * @return true if the file exist, else false.
     */
    private static boolean isFileExists(String fileName) {
        File f = new File(fileName);
        return f.exists() && !f.isDirectory();
    }

    /***
     * Reads the default properties from the classpath. Used for tests and also used to merge properties from a file.
     *
     * @return - The read properties, named localProperties within the scope of the method
     * @throws java.io.IOException
     */
    public Properties readDefaultProperties() throws IOException {
        Properties localProperties = new Properties();
        URL defaultConfigURL = Config.class.getClassLoader().getResource(CONFIG_FILE_NAME);
        if (defaultConfigURL != null) {
            try (InputStream input = this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
                LOGGER.info("Reading default properties from {}", defaultConfigURL.getFile());
                localProperties.load(input);
            }
        } else {
            throw new TechnicalException("Default properties file " + CONFIG_FILE_NAME + " could not be found");
        }
        return localProperties;
    }

    /**
     * Tries to read the application properties.
     *
     * @return - A collection of properties, which is empty if no application has been defined (i.e. at unit tests)
     */
    private static Properties readApplicationProperties() {
        Properties localProperties = new Properties();
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("/" + APPLICATION_PROPERTIES)) {
            localProperties.load(input);
        } catch (IOException | NullPointerException e) {
            LOGGER.warn("The file {} could not be read.", APPLICATION_PROPERTIES);
        }
        return localProperties;
    }

    /**
     * Returns the configuration folder of the application or for unit tests.
     *
     * @return the configuration Folder
     */
    public static String getConfigurationFolder() {
        String folderName;
        if (isDeployed()) {
            Properties localProperties = readApplicationProperties();
            String applicationName = localProperties.getProperty(APPLICATION_NAME_PROPERTY);
            folderName = DOMAIN_CONFIG_FOLDER + File.separator + applicationName + File.separator;
        } else {
            folderName = SRC_TEST_RESOURCES_FOLDER;
        }
        return folderName;
    }

    /**
     * Determines whether a deployed application is running.
     *
     * @return true when a deployed application is running, false otherwise
     */
    private static boolean isDeployed() {
        return Config.class.getClassLoader().getResource("/" + APPLICATION_PROPERTIES) != null;
    }

    /**
     * Gets a property value as an {@link Integer}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public Integer getIntegerProperty(ConfigParam configParam) {
        return Integer.parseInt(properties.getProperty(configParam.name()));
    }

    /**
     * Gets a property value as a {@link Long}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public Long getLongProperty(ConfigParam configParam) {
        return Long.parseLong(properties.getProperty(configParam.name()));
    }

    /**
     * Reads properties from a file.
     *
     * @param configFilename the filename of the properties file
     * @return - The read properties
     */
    private static Properties readPropertiesFromFile(String configFilename) throws IOException {
        Properties properties = new Properties();
        if (!isFileExists(configFilename)) {
            LOGGER.warn("Can not find properties file: {}", configFilename);
        } else {
            LOGGER.info("Reading properties file: {}", configFilename);
            try (FileInputStream input = new FileInputStream(configFilename)) {
                properties.load(input);
            }
        }
        return properties;
    }
}
