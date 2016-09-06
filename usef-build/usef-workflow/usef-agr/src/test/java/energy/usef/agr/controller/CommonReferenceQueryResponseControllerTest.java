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

import java.io.IOException;
import java.io.StringWriter;

import javax.enterprise.event.Event;

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

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.workflow.plan.connection.profile.AgrUpdateElementDataStoreEvent;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.helper.MessageMetadataBuilder;

/**
 * Text class for CommonReferenceQueryController.
 */
@RunWith(PowerMockRunner.class)
public class CommonReferenceQueryResponseControllerTest {

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private MessageService messageService;

    @Mock
    private ConfigAgr configAgr;

    @Mock
    private Event<AgrUpdateElementDataStoreEvent> agrUpdateElementDataStoreEventManager;

    private CommonReferenceQueryResponseController controller;

    @Before
    public void init() {
        controller = new CommonReferenceQueryResponseController();
        Whitebox.setInternalState(controller, corePlanboardBusinessService);
        Whitebox.setInternalState(controller, messageService);
        Whitebox.setInternalState(controller, configAgr);
        Whitebox.setInternalState(controller, "agrUpdateElementDataStoreEventManager", agrUpdateElementDataStoreEventManager);
        PowerMockito.when(messageService.getInitialMessageOfConversation(Matchers.any(String.class))).then(invocation -> {
            Message message = new Message();
            message.setCreationTime(new LocalDateTime(2015, 1, 1, 1, 0, 0));
            message.setXml(fetchFileContent("energy/usef/agr/controller/common_reference_query.xml"));
            message.setConversationId((String) invocation.getArguments()[0]);
            return message;
        });
        PowerMockito.when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL)).thenReturn(2);
    }

    /**
     * Tests CommonReferenceQueryController.action method with a success response.
     *
     * @throws BusinessException
     */
    @Test
    public void testSuccessResponseAction() throws BusinessException {
        Mockito.when(messageService.hasEveryCommonReferenceQuerySentAResponseReceived(Matchers.any(LocalDateTime.class)))
                .thenReturn(true);
        CommonReferenceQueryResponse message = new CommonReferenceQueryResponse();
        message.setMessageMetadata(new MessageMetadataBuilder().conversationID().build());
        message.setResult(DispositionSuccessFailure.SUCCESS);
        controller.action(message, null);
        Mockito.verify(messageService, Mockito.times(1)).hasEveryCommonReferenceQuerySentAResponseReceived(
                Matchers.any(LocalDateTime.class));
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .storeCommonReferenceQueryResponse(Matchers.eq(message), Matchers.eq(CommonReferenceEntityType.BRP),
                        Matchers.eq(new LocalDate(2015, 1, 2)), Matchers.eq(2));
        Mockito.verify(agrUpdateElementDataStoreEventManager, Mockito.times(1))
                .fire(Matchers.any(AgrUpdateElementDataStoreEvent.class));
    }

    /**
     * Tests CommonReferenceQueryController.action method with a failed response.
     *
     * @throws BusinessException
     */
    @Test
    public void testFailedResponseAction() throws BusinessException {
        CommonReferenceQueryResponse message = new CommonReferenceQueryResponse();
        message.setResult(DispositionSuccessFailure.FAILURE);
        controller.action(message, null);
        Mockito.verify(corePlanboardBusinessService, Mockito.times(0))
                .storeCommonReferenceQueryResponse(Matchers.eq(message), Matchers.eq(CommonReferenceEntityType.BRP),
                        Matchers.eq(new LocalDate(2015, 2, 1)), Matchers.eq(2));
        Mockito.verify(messageService, Mockito.times(0)).hasEveryCommonReferenceQuerySentAResponseReceived(
                Matchers.any(LocalDateTime.class));
        Mockito.verifyZeroInteractions(agrUpdateElementDataStoreEventManager);
    }

    private String fetchFileContent(String filePath) throws IOException {
        StringWriter xmlWriter = new StringWriter();
        IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath), xmlWriter);
        return xmlWriter.toString();
    }

}
