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

package energy.usef.agr.controller;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import energy.usef.agr.util.ReflectionUtil;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOfferRevocationResponse;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.helper.MessageMetadataBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

/**
 * Test class in charge of the unit tests related to {@link FlexOfferRevocationResponseController}.
 */
@RunWith(PowerMockRunner.class)
public class FlexOfferRevocationResponseControllerTest {
    /**
     * 
     */
    private static final String PARTICIPANT_DOMAIN = "usef-example.com";
    private static final long FLEX_OFFER_SEQUENCE = 1234567;
    private static final String MESSAGE_RECORD_UPDATED_LOG = "Corresponding plan board message records updated for the flex offer sequence number";
    private static final String MESSAGE_RECORD_WRONG_DATA_LOG = "Related plan board message for the flex offer sequence number";
    private static final String MESSAGE_RECORD_EMPTY_DATA_LOG = "No corresponding plan board message found for the flex offer sequence number";
    private static final String MESSAGE_REJECTED_LOG = "has been rejected";

    @Mock
    private Logger LOGGER;

    @Mock
    private MessageService messageService;

    @Mock
    private CorePlanboardBusinessService planboardBusinessService;

    private FlexOfferRevocationResponseController controller;

    @Before
    public void init() throws Exception {
        controller = new FlexOfferRevocationResponseController();
        ReflectionUtil.setFinalStatic(FlexOfferRevocationResponseController.class.getDeclaredField("LOGGER"), LOGGER);
        Whitebox.setInternalState(controller, messageService);
        Whitebox.setInternalState(controller, planboardBusinessService);
        PowerMockito.when(messageService.getInitialMessageOfConversation(Matchers.any(String.class))).thenReturn(new Message());
    }

    @Test
    public void testActionOnAccepted() throws BusinessException {
        Mockito.when(planboardBusinessService.findSinglePlanboardMessage(FLEX_OFFER_SEQUENCE,
                        DocumentType.FLEX_OFFER, PARTICIPANT_DOMAIN)).thenReturn(buildPlanboardMessage(DocumentStatus.ACCEPTED));

        controller.action(buildFlexOfferRevocationResponse(DispositionAcceptedRejected.ACCEPTED), null);

        verify(LOGGER, times(1)).debug(contains(MESSAGE_RECORD_UPDATED_LOG), anyLong());
    }

    @Test
    public void testActionWithEmptyPlanboardMessageData() throws BusinessException {
        Mockito.when(planboardBusinessService
                .findSinglePlanboardMessage(FLEX_OFFER_SEQUENCE, DocumentType.FLEX_OFFER, PARTICIPANT_DOMAIN))
                .thenReturn(null);

        controller.action(buildFlexOfferRevocationResponse(DispositionAcceptedRejected.ACCEPTED), null);

        verify(LOGGER, times(1)).error(contains(MESSAGE_RECORD_EMPTY_DATA_LOG), anyLong());
    }

    @Test
    public void testActionWithWrongPlanboardMessageData() throws BusinessException {
        Mockito.when(planboardBusinessService.findSinglePlanboardMessage(FLEX_OFFER_SEQUENCE,
                        DocumentType.FLEX_OFFER, PARTICIPANT_DOMAIN)).thenReturn(buildPlanboardMessage(DocumentStatus.SENT));

        controller.action(buildFlexOfferRevocationResponse(DispositionAcceptedRejected.ACCEPTED), null);

        verify(LOGGER, times(1)).error(contains(MESSAGE_RECORD_WRONG_DATA_LOG), anyLong());
    }

    @Test
    public void testActionOnRejected() throws BusinessException {
        Mockito.when(planboardBusinessService.findSinglePlanboardMessage(FLEX_OFFER_SEQUENCE,
                        DocumentType.FLEX_OFFER, PARTICIPANT_DOMAIN)).thenReturn(buildPlanboardMessage(DocumentStatus.ACCEPTED));

        controller.action(buildFlexOfferRevocationResponse(DispositionAcceptedRejected.REJECTED), null);

        verify(LOGGER, times(1)).warn(contains(MESSAGE_REJECTED_LOG), anyLong(), anyString(), anyString());
    }

    private PlanboardMessage buildPlanboardMessage(DocumentStatus status) {
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentStatus(status);
        planboardMessage.setParticipantDomain(PARTICIPANT_DOMAIN);
        return planboardMessage;
    }

    private FlexOfferRevocationResponse buildFlexOfferRevocationResponse(DispositionAcceptedRejected disposition) {
        FlexOfferRevocationResponse response = new FlexOfferRevocationResponse();
        response.setMessageMetadata(MessageMetadataBuilder.buildDefault());
        response.getMessageMetadata().setSenderDomain(PARTICIPANT_DOMAIN);
        response.setResult(disposition);
        response.setSequence(FLEX_OFFER_SEQUENCE);
        return response;
    }
}
