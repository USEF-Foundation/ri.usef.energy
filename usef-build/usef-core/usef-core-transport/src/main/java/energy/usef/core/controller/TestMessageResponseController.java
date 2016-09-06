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

package energy.usef.core.controller;

import energy.usef.core.data.xml.bean.message.TestMessageResponse;
import energy.usef.core.model.Message;

import javax.ejb.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes incoming test response messages.
 */
@Singleton
public class TestMessageResponseController extends BaseIncomingResponseMessageController<TestMessageResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestMessageResponseController.class);

    /**
     * {@inheritDoc}
     */
    public void action(TestMessageResponse response, Message savedMessage) {
        LOGGER.info("Test message response received");
    }

}
