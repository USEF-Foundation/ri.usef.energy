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

import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexRequestResponse;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.helper.MessageMetadataBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link FlexRequestResponseController} class.
 */
@RunWith(PowerMockRunner.class)
public class FlexRequestResponseControllerTest {
    @Mock
    private MessageService messageService;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    private FlexRequestResponseController controller;

    @Before
    public void init() {
        controller = new FlexRequestResponseController();
        Whitebox.setInternalState(controller, messageService);
        Whitebox.setInternalState(controller, corePlanboardBusinessService);

        PowerMockito.when(messageService.getInitialMessageOfConversation(Matchers.any(String.class))).thenReturn(new Message());
    }

    @Test
    public void testActionOnAccepted() throws BusinessException {
        PlanboardMessage controlPlanboardMessage = new PlanboardMessage();
        controlPlanboardMessage.setDocumentStatus(DocumentStatus.SENT);
        PowerMockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(Matchers.any(Long.class), Matchers.eq(
                DocumentType.FLEX_REQUEST), Matchers.eq("agr-usef-example.com"))).thenReturn(controlPlanboardMessage);
        controller.action(buildFlexRequestResponse(DispositionAcceptedRejected.ACCEPTED), null);
        Assert.assertEquals(DocumentStatus.ACCEPTED, controlPlanboardMessage.getDocumentStatus());
    }

    @Test
    public void testActionOnRejected() throws BusinessException {
        PlanboardMessage controlPlanboardMessage = new PlanboardMessage();
        controlPlanboardMessage.setDocumentStatus(DocumentStatus.SENT);
        PowerMockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(Matchers.any(Long.class), Matchers.eq(
                DocumentType.FLEX_REQUEST), Matchers.eq("agr-usef-example.com"))).thenReturn(controlPlanboardMessage);
        controller.action(buildFlexRequestResponse(DispositionAcceptedRejected.REJECTED), null);
        Assert.assertEquals(DocumentStatus.REJECTED, controlPlanboardMessage.getDocumentStatus());
    }

    private FlexRequestResponse buildFlexRequestResponse(DispositionAcceptedRejected disposition) {
        FlexRequestResponse response = new FlexRequestResponse();
        response.setMessageMetadata(MessageMetadataBuilder.build("brp.usef-example.com", USEFRole.AGR,
                "agr-usef-example.com", USEFRole.BRP, null).build());
        response.setResult(disposition);
        return response;
    }
}
