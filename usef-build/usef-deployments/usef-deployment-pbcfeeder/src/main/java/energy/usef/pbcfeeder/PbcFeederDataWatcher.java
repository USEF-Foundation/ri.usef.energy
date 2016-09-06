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

package energy.usef.pbcfeeder;

import energy.usef.core.config.AbstractConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is in charge of watching the file containing the data for the PBCFeeder component and to reload it upon changes.
 */
@Startup
@Singleton
public class PbcFeederDataWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(PbcFeederDataWatcher.class);
    private static final String PBC_FEEDER_FILE_NAME = "stubinputdata.xls";

    @Inject
    private PbcFeeder pbcFeeder;

    private ExecutorService watcherExecutor = Executors.newSingleThreadExecutor();

    /**
     * Constructs a filewatcher to be notified of the changes occuring in the file providing the data to the PbcFeeder.
     */
    @PostConstruct
    public void init() {
        // initialize the pbc feeder.
        LOGGER.info("Loading PbcFeederData a first time.");
        pbcFeeder.readFile(
                new File(System.getProperty(AbstractConfig.CONFIG_FOLDER_PROPERTY)).toPath().resolve(PBC_FEEDER_FILE_NAME));
        // register a file watcher.
        LOGGER.info("Registering a file watcher for the PbcFeederData.");
        watcherExecutor.submit(new FileWatcher());

    }

    private class FileWatcher implements Runnable {

        private final Path directoryToWatch;
        private final Path fileToWatch;

        public FileWatcher() {
            directoryToWatch = new File(System.getProperty(AbstractConfig.CONFIG_FOLDER_PROPERTY)).toPath();
            fileToWatch = directoryToWatch.resolve(PBC_FEEDER_FILE_NAME);
        }

        @Override
        public void run() {
            try {
                WatchService watcher = FileSystems.getDefault().newWatchService();
                directoryToWatch.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                while (true) {
                    handleOneEvent(watcher.take(), directoryToWatch);
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Exception caught in the FileWatcher of the PBC Feeder Excel sheet.", e);
            }
        }

        @SuppressWarnings("unchecked") private void handleOneEvent(WatchKey key, Path directoryToWatch) {
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event == null || event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                LOGGER.debug("Modification of the file occurred: {}", event.context());
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                try {
                    if (PBC_FEEDER_FILE_NAME.equalsIgnoreCase(
                            directoryToWatch.resolve(pathEvent.context()).toRealPath().getFileName().toString())) {
                        LOGGER.info("Modification of the PBC Feeder file occurred: {}", event.context());
                        pbcFeeder.readFile(fileToWatch);
                    }
                } catch (IOException e) {
                    // This exception has no functional impact. It may happen due to the saving process of MS Excel.
                    LOGGER.debug("Exception caught while listening to the modification event of the PBC Feeder Excel sheet.", e);
                }
            }
            key.reset();
        }
    }
}
