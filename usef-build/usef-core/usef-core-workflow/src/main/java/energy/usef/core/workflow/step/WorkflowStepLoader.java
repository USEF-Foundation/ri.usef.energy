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

package energy.usef.core.workflow.step;

import energy.usef.core.config.AbstractConfig;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.workflow.WorkflowStep;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class loads the workflow step configurations and register changes.
 */
@Startup
@Singleton
public class WorkflowStepLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowStepLoader.class);
    private static final String FILE_PBC_CATALOG = "pbc-catalog.properties";

    private Map<String, Class<WorkflowStep>> workflowStepsMap = new HashMap<>();

    private ExecutorService watcherExecutor = Executors.newSingleThreadExecutor();

    @Inject
    @Any
    private Instance<WorkflowStep> myBeans;

    /**
     * Initialization method.
     */
    @PostConstruct
    public void init() {
        loadPbcConfig();
        watcherExecutor.submit(new ConfigWatcher(this));
    }

    private void loadPbcConfig() {
        try {
            Map<String, Class<WorkflowStep>> newlyMappedWorkflowSteps = new HashMap<>();
            File pbcCatalogInConfig = new File(AbstractConfig.getConfigurationFolder() + FILE_PBC_CATALOG);
            InputStream inputStream;
            if (pbcCatalogInConfig.exists() && !pbcCatalogInConfig.isDirectory()) {
                LOGGER.info("Using PBC catalog file {}.", pbcCatalogInConfig.getAbsolutePath());
                inputStream = new FileInputStream(pbcCatalogInConfig);
            } else {
                LOGGER.warn("PBC catalog  file {} not found, using default.", pbcCatalogInConfig.getAbsolutePath());
                inputStream = this.getClass().getClassLoader().getResourceAsStream(FILE_PBC_CATALOG);
            }
            // load the properties
            Properties properties = new Properties();
            properties.load(inputStream);
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (StringUtils.isEmpty((String)entry.getValue())) {
                    LOGGER.error("Invalid configuration for WorkflowStep {}, class name can not be empty.", entry.getKey());
                }
                Class<WorkflowStep> clazz = (Class<WorkflowStep>) Class.forName((String) entry.getValue());

                if (!hasWorkflowStepInterface(clazz)) {
                    LOGGER.error("Keeping old configuration, error occurred: Class [{}] does not implement WorkflowStep interface.",
                            clazz);
                    return;
                }
                newlyMappedWorkflowSteps.put((String) entry.getKey(), clazz);
            }
            // no exceptions occurred, override current map
            this.workflowStepsMap = newlyMappedWorkflowSteps;
            newlyMappedWorkflowSteps
                    .forEach((name, clazz) -> LOGGER.info("Successfully Loaded Step [{}] with Class [{}]", name, clazz));
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.error("Keeping old configuration, exception occurred: " + e.getMessage(), e);
        }
    }

    private boolean hasWorkflowStepInterface(Class<?> clazz) {
        return Stream.of(clazz.getInterfaces())
                .anyMatch(interfaceClazz -> interfaceClazz.toString().equals(WorkflowStep.class.toString()));
    }

    public Class<WorkflowStep> getWorkflowStep(String workflowStepName) {
        return workflowStepsMap.get(workflowStepName);
    }

    private class ConfigWatcher implements Runnable {

        private WorkflowStepLoader stepLoader;

        /**
         * Constructor.
         * 
         * @param stepLoader step loader
         */
        public ConfigWatcher(WorkflowStepLoader stepLoader) {
            this.stepLoader = stepLoader;
        }

        private void handleOneEvent(WatchKey key, Path directoryToWatch) throws IOException {
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event == null) {
                    continue;
                }
                LOGGER.debug("Modification of the PBC catalog file occurred: {}", event.context());
                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                if (FILE_PBC_CATALOG.equalsIgnoreCase(directoryToWatch.resolve(pathEvent.context()).toRealPath()
                        .getFileName().toString())) {
                    LOGGER.info("Modification of the PBC catalog file occurred: {}", event.context());
                    stepLoader.loadPbcConfig();
                }
            }
            key.reset();
        }

        @Override
        public void run() {
            try {
                LOGGER.info("Registering a new WatchService to detect changes in the PBC catalog.");
                WatchService watcher = FileSystems.getDefault().newWatchService();
                Path directoryToWatch = new File(AbstractConfig.getConfigurationFolder()).toPath();
                directoryToWatch.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                while (true) {
                    handleOneEvent(watcher.take(), directoryToWatch);
                }
            } catch (IOException e) {
                LOGGER.error("Error during initialization of the WatcherService for the PBC catalog.", e);
                throw new TechnicalException(e);
            } catch (InterruptedException e) {
                LOGGER.warn("Config Watcher service interrupted!");
            }
        }

    }

}
