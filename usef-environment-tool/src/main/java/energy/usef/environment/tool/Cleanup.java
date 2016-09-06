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
import energy.usef.environment.tool.util.FileUtil;

import java.io.IOException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cleanup script which cleans the domain directory entirely.
 */
public class Cleanup {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cleanup.class);

    public void run() throws IOException {
        if (FileUtil.isFolderExists(ToolConfig.getUsefEnvironmentNodesFolder())) {
            boolean remove = askToCleanup();
            if (remove) {
                LOGGER.info("Removing folder: " + ToolConfig.getUsefEnvironmentNodesFolder());
                FileUtil.removeFolders(ToolConfig.getUsefEnvironmentNodesFolder());
                LOGGER.info("Cleanup executed... and ended.");
            } else {
                LOGGER.info("The cleanup script canceled.");
            }
        } else {
            LOGGER.info("The folder " + ToolConfig.getUsefEnvironmentNodesFolder()
                    + " does not exist! Unnecessary to remove this folder. Cleanup is ended.");
        }
    }

    @SuppressWarnings("resource")
    private boolean askToCleanup() {
        Scanner input = new Scanner(System.in);
        System.out.println("\nAre you sure to remove the nodes-folder [yes/no]?");
        String confirmation = input.nextLine();
        return (confirmation != null && ("yes".equalsIgnoreCase(confirmation.trim()) || "y".equalsIgnoreCase(confirmation.trim())));
    }

    public static void main(String[] args) throws IOException {
        new Cleanup().run();
    }

}
