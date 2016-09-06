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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Startup Bean to initiate UDP Server to send the USEF time where needed.
 */
@Startup
@Singleton
public class TimeServer {

    private ExecutorService timeServerThreadPool = Executors.newSingleThreadExecutor();
    private TimeServerThread timeServerThread;
    public static final String CONFIG_FILE_NAME = "time-config.properties";

    /**
     * Starts the TimeServer with the chosen properties.
     * 
     * @throws IOException
     */
    @PostConstruct
    public void startTimeServer() throws IOException {
        try {
            Config config = new Config();
            Integer serverPort = config.getIntegerProperty(ConfigParam.SERVER_PORT);
            Long timeFactor = config.getLongProperty(ConfigParam.TIME_FACTOR);
            timeServerThread = new TimeServerThread(timeFactor, serverPort);
            timeServerThreadPool.execute(timeServerThread);
        } catch (SocketException e) {
            throw new RuntimeException("Unable to start timeServer: " + e.getMessage(), e);
        }
    }

    /**
     * Stop's the timeServerThread.
     */
    @PreDestroy
    public void stopTimeServer() {
        timeServerThread.shutdown();
        timeServerThreadPool.shutdownNow();
    }
}
