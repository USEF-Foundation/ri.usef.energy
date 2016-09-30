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

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.repository.MessageErrorRepository;
import energy.usef.core.repository.MessageRepository;
import energy.usef.core.repository.SignedMessageHashRepository;

/**
 * Business service class in charge of housekeeping operations concerning transport layer.
 */
@Stateless
public class TransportHousekeepingBusinessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransportHousekeepingBusinessService.class);

    @Inject
    private MessageErrorRepository messageErrorRepository;

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private SignedMessageHashRepository signedMessageHashRepository;

    /**
     * Cleanup core transport layer data for a given period.
     *
     * @param period
     */
    public void cleanup(LocalDate period) {

        int messageErrorCount = messageErrorRepository.cleanup(period);
        LOGGER.info("Cleaned up {} MessageError objects", messageErrorCount);

        int messageCount = messageRepository.cleanup(period);
        LOGGER.info("Cleaned up {} Message objects.", messageCount);

        int signedMessageHashCount = signedMessageHashRepository.cleanup(period);
        LOGGER.info("Cleaned up {} SignedMessageHash objects.", signedMessageHashCount);
    }
}
