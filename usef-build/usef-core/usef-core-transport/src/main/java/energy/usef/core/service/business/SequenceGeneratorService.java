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

import java.util.concurrent.atomic.AtomicLong;

import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.joda.time.format.DateTimeFormat;

/**
 * This service is in charge of the generation of unique sequence numbers that can be used in messages. The format of a sequence
 * number is based on dates (yyyymmddHHMMSSsss).
 */
@Startup
@Singleton
public class SequenceGeneratorService {

    private static final String TIME_SEQUENCE_FORMAT = "yyyyMMddHHmmssSSS";

    private final AtomicLong sequenceHolder;

    /**
     * Default constructor.
     */
    public SequenceGeneratorService() {
        sequenceHolder = new AtomicLong(Long.valueOf(DateTimeUtil.getCurrentDateTime().toString(DateTimeFormat.forPattern(TIME_SEQUENCE_FORMAT))));
    }

    /**
     * Get the next sequence number (after incrementation of 1).
     *
     * @return a {@link Long}.
     */
    public Long next() {
        return sequenceHolder.incrementAndGet();
    }

}
