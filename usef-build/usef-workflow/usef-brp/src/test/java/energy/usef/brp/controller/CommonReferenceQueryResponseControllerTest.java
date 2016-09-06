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

package energy.usef.brp.controller;

import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.helper.MessageMetadataBuilder;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link CommonReferenceQueryResponseController} class.
 */
@RunWith(PowerMockRunner.class)
public class CommonReferenceQueryResponseControllerTest {

    private CommonReferenceQueryResponseController controller;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private ConfigBrp configBrp;

    @Mock
    private MessageService messageService;

    @Before
    public void init() {
        controller = new CommonReferenceQueryResponseController();
        Whitebox.setInternalState(controller, configBrp);
        Whitebox.setInternalState(controller, corePlanboardBusinessService);
        Whitebox.setInternalState(controller, messageService);
        PowerMockito.when(configBrp.getIntegerProperty(ConfigBrpParam.BRP_INITIALIZE_PLANBOARD_DAYS_AHEAD)).thenReturn(1);
        PowerMockito.when(configBrp.getIntegerProperty(ConfigBrpParam.BRP_INITIALIZE_PLANBOARD_DAYS_INTERVAL)).thenReturn(2);
        PowerMockito.when(messageService.getInitialMessageOfConversation(Matchers.any(String.class))).then(invocation -> {
            Message message = new Message();
            message.setCreationTime(new LocalDateTime(2015, 1, 1, 1, 0, 0));
            message.setXml(fetchFileContent("energy/usef/brp/controller/common_reference_query.xml"));
            message.setConversationId((String) invocation.getArguments()[0]);
            return message;
        });
    }

    @Test
    public void testActionIsSuccessful() throws BusinessException {
        controller.action(buildCommonReferenceQueryResponse(), null);
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .storeCommonReferenceQueryResponse(Matchers.any(CommonReferenceQueryResponse.class),
                        Matchers.any(CommonReferenceEntityType.class), Matchers.eq(new LocalDate(2015, 1, 2)), Matchers.eq(2));
    }

    private CommonReferenceQueryResponse buildCommonReferenceQueryResponse() {
        CommonReferenceQueryResponse response = new CommonReferenceQueryResponse();
        response.setResult(DispositionSuccessFailure.SUCCESS);
        response.setMessageMetadata(new MessageMetadataBuilder().senderDomain("cro.usef-example.com")
                .senderRole(USEFRole.CRO)
                .recipientDomain("brp.usef-example.com")
                .recipientRole(USEFRole.BRP)
                .messageID()
                .conversationID()
                .timeStamp()
                .precedence(ROUTINE)
                .build());
        return response;
    }

    private String fetchFileContent(String filePath) throws IOException {
        StringWriter xmlWriter = new StringWriter();
        IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath), xmlWriter);
        return xmlWriter.toString();
    }
}
