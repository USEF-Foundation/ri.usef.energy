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

package energy.usef.agr.controller.error;

import energy.usef.agr.util.ReflectionUtil;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.MessagePrecedence;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.service.helper.MessageMetadataBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

@RunWith(PowerMockRunner.class)
public class CommonReferenceQueryErrorControllerTest {

    private static final String ERROR_MESSAGE = "Start processing not sent CommonReferenceQuery message.";

    private CommonReferenceQueryErrorController controller;
    @Mock
    private Logger LOGGER;

    @Before
    public void init() throws Exception {
        controller = new CommonReferenceQueryErrorController();
        ReflectionUtil.setFinalStatic(CommonReferenceQueryErrorController.class.getDeclaredField("LOGGER"), LOGGER);
    }

    @Test
    public void testExecute() throws Exception {
        controller.execute(buildCommonReferenceQueryMessage());
        // verify that logger has been called (only logic at the moment).
        Mockito.verify(LOGGER, Mockito.times(1)).warn(ERROR_MESSAGE);
    }

    private CommonReferenceQuery buildCommonReferenceQueryMessage() {
        CommonReferenceQuery commonReferenceQuery = new CommonReferenceQuery();
        commonReferenceQuery.setMessageMetadata(buildMessageMetadata());
        commonReferenceQuery.setEntity(CommonReferenceEntityType.AGGREGATOR);
        return commonReferenceQuery;
    }

    private MessageMetadata buildMessageMetadata() {
        return new MessageMetadataBuilder().conversationID().messageID()
                .senderDomain("agr.usef-example.com").senderRole(USEFRole.AGR)
                .recipientDomain("cro.usef-example.com").recipientRole(USEFRole.CRO)
                .precedence(MessagePrecedence.TRANSACTIONAL)
                .build();
    }
}
