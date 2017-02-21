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
import javax.ejb.Singleton;
import javax.ejb.Startup;
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
     * Initialize a bean after the instance has been constructed.
     */
    @PostConstruct
    public void initBean() {
        LOGGER.info("Check if the libsodium can be loaded during startup.");
        NaCl.sodium();
    }

}
