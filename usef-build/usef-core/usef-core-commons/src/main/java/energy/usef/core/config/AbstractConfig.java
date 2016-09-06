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

package energy.usef.core.config;

import energy.usef.core.exception.TechnicalException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract config file which can be used in every participant project.
 */
public abstract class AbstractConfig {

    private static final String SRC_TEST_RESOURCES_FOLDER = "src/test/resources/";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfig.class);

    public static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String APPLICATION_NAME_PROPERTY = "APPLICATION_NAME";

    public static final String CONFIG_FILE_NAME = "config.properties";
    protected static final String CONFIG_LOCAL_FILE_NAME = "config-local.properties";

    public static final String CONFIG_FOLDER_PROPERTY = "jboss.server.config.dir";
    public static final String DOMAIN_CONFIG_FOLDER = System.getProperty(CONFIG_FOLDER_PROPERTY);

    private Thread configFileWatcherThread;
    protected Properties properties = new Properties();

    public Properties getProperties() {
        return properties;
    }

    /**
     * Tries to find a file as a resource. A TechnicalException is thrown if it couldn't be found.
     *
     * @param fullFileName the full name where the file is located.
     * @param filenameOfTheList the name of the allow- or denylist.
     * @return The InputStream representing the resource.
     */
    public InputStream findFile(String fullFileName, String filenameOfTheList) {
        InputStream is;

        File file = new File(fullFileName);
        if (file.exists()) {
            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new TechnicalException(e);
            }
        } else {
            // try to read the allow- or deny list from the application specific location.
            String configFile = getConfigurationFolder() + filenameOfTheList;
            if (isFileExists(configFile)) {
                try {
                    is = new FileInputStream(new File(configFile));
                } catch (FileNotFoundException e) {
                    throw new TechnicalException(e);
                }
            } else {
                LOGGER.warn("The file {} can not be found, trying classpath. ", file.getAbsolutePath());
                is = Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(fullFileName);
                if (is == null) {
                    throw new TechnicalException("Unable to load file " + fullFileName);
                }
            }
        }
        return is;
    }

    protected void startConfigWatcher() {
        configFileWatcherThread = createWatcherService();
        configFileWatcherThread.start();
    }

    protected void stopConfigWatcher() {
        // Clean up the thread
        if (configFileWatcherThread != null) {
            configFileWatcherThread.interrupt();
            configFileWatcherThread = null;
        }
    }

    /**
     * The configuration watcher watches if a configuration file changes. If so, the properties file is loaded again and existing
     * properties are overwritten.
     *
     * @return true if the configuration watcher is running.
     */
    public boolean isConfigWatcherRunning() {
        return configFileWatcherThread != null;
    }

    private final class ConfigWatcher implements Runnable {

        private void handleOneEvent(WatchKey key, Path directoryToWatch) throws IOException {
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event == null) {
                    continue;
                }
                LOGGER.debug("Modification of the file occurred: {}", event.context());
                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                if (CONFIG_LOCAL_FILE_NAME.equalsIgnoreCase(directoryToWatch.resolve(pathEvent.context()).toRealPath()
                        .getFileName().toString())) {
                    LOGGER.info("Modification of the config file occurred: {}", event.context());
                    readProperties();
                }
            }
            key.reset();
        }

        @Override
        public void run() {
            try {
                LOGGER.info("Registering a new WatchService to detect changes in the Config.properties");
                WatchService watcher = FileSystems.getDefault().newWatchService();
                Path directoryToWatch = new File(getConfigurationFolder()).toPath();
                directoryToWatch.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                while (true) {
                    handleOneEvent(watcher.take(), directoryToWatch);
                }
            } catch (IOException e) {
                LOGGER.error("Error during initialization of the WatcherService of the configuration.", e);
                throw new TechnicalException(e);
            } catch (InterruptedException e) {
                LOGGER.warn("Config Watcher service interrupted!");
            }
        }

    }

    private Thread createWatcherService() {
        // create a watch service to detect changes in the property file
        return new Thread(new ConfigWatcher());
    }

    /**
     * Returns the configuration folder of the application or for unit tests.
     *
     * @return the configuration Folder
     */
    public static String getConfigurationFolder() {
        String foldername;
        if (isDeployed()) {
            Properties localProperties = readApplicationProperties();
            String applicationName = localProperties.getProperty(APPLICATION_NAME_PROPERTY);
            foldername = DOMAIN_CONFIG_FOLDER + File.separator + applicationName + File.separator;
        } else {
            foldername = SRC_TEST_RESOURCES_FOLDER;
        }
        return foldername;
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
            for (Entry<Object, Object> entry : defaults.entrySet()) {
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
     * Reads the default properties from the classpath. Used for tests and also used to merge properties from a file.
     *
     * @return - The read properties, named localProperties within the scope of the method
     * @throws IOException
     */
    public Properties readDefaultProperties() throws IOException {
        Properties localProperties = new Properties();
        URL defaultConfigURL = AbstractConfig.class.getClassLoader().getResource(CONFIG_FILE_NAME);
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
     * Determines whether a deployed application is running.
     *
     * @return true when a deployed application is running, false otherwise
     */
    private static boolean isDeployed() {
        return AbstractConfig.class.getClassLoader().getResource("/" + APPLICATION_PROPERTIES) != null;
    }

    /**
     * Tries to read the application properties.
     *
     * @return - A collection of properties, which is empty if no application has been defined (i.e. at unit tests)
     */
    private static Properties readApplicationProperties() {
        Properties localProperties = new Properties();
        try (InputStream input = AbstractConfig.class.getClassLoader().getResourceAsStream("/" + APPLICATION_PROPERTIES)) {
            localProperties.load(input);
        } catch (IOException | NullPointerException e) {
            LOGGER.warn("The file {} could not be read.", APPLICATION_PROPERTIES, e);
        }
        return localProperties;
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
