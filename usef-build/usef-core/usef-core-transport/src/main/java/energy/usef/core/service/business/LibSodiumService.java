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

package energy.usef.core.service.business;

import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.encryption.NaCl;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This service is in charge of the generation of unique sequence numbers that can be used in messages. The format of a sequence
 * number is based on dates (yyyymmddHHMMSSsss).
 */
@Startup
@Singleton
public class LibSodiumService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibSodiumService.class);

    /**
     * Initialize NaCl Sodium library. Sometimes this one is timed out and the deployment is
     * canceled.
     * @param timer of the event which is fired at once.
     */
    @Schedule(hour = "*", minute = "*", persistent = false)
    protected void init(Timer timer) {
        LOGGER.info("Check if the Libsodium can be loaded during startup.");
        NaCl.sodium();

        // Timer is canceled to prevent it is thrown again.
        timer.cancel();
    }
}
