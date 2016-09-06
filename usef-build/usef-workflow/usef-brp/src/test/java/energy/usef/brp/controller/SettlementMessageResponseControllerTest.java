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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import energy.usef.brp.workflow.settlement.send.BrpSettlementMessageResponseCoordinator;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedDisputed;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOrderSettlementStatus;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.SettlementMessageResponse;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.Message;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.helper.MessageMetadataBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit-test for SettlementMessageResponseController class
 */
@RunWith(PowerMockRunner.class)
public class SettlementMessageResponseControllerTest {

    private SettlementMessageResponseController controller;

    @Mock
    private BrpSettlementMessageResponseCoordinator coordinator;
    @Mock
    private MessageService messageService;

    @Before
    public void setUp() throws Exception {
        controller = new SettlementMessageResponseController();
        Whitebox.setInternalState(controller, coordinator);
        Whitebox.setInternalState(controller, messageService);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testActionWithDispositionAccept() throws Exception {
        SettlementMessageResponse response = buildSettlementMessageResponse();
        response.setResult(DispositionAcceptedRejected.ACCEPTED);
        controller.action(response, null);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> acceptedListCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> disputedListCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(coordinator, Mockito.times(1)).processPtuSettlements(acceptedListCaptor.capture(),
                Matchers.eq(DispositionAcceptedDisputed.ACCEPTED), Matchers.eq("agr.usef-example.com"));
        Mockito.verify(coordinator, Mockito.times(1)).processPtuSettlements(disputedListCaptor.capture(),
                Matchers.eq(DispositionAcceptedDisputed.DISPUTED), Matchers.eq("agr.usef-example.com"));
        List<Long> acceptedList = acceptedListCaptor.getValue();
        List<Long> disputedList = disputedListCaptor.getValue();
        assertEquals(1, acceptedList.size());
        assertEquals(1, disputedList.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testActionWithDispositionReject() throws Exception {
        Mockito.when(messageService.getMessageResponseByConversationId(Matchers.any(String.class))).thenReturn(
                buildSavedSettlementMessage());

        SettlementMessageResponse response = buildSettlementMessageResponse();
        response.setResult(DispositionAcceptedRejected.REJECTED);
        controller.action(response, null);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> disputedListCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(coordinator, Mockito.times(0)).processPtuSettlements(Matchers.any(List.class),
                Matchers.eq(DispositionAcceptedDisputed.ACCEPTED), Matchers.any(String.class));
        Mockito.verify(coordinator, Mockito.times(1)).processPtuSettlements(disputedListCaptor.capture(),
                Matchers.eq(DispositionAcceptedDisputed.DISPUTED), Matchers.eq("agr.usef-example.com"));
        List<Long> disputedList = disputedListCaptor.getValue();
        assertEquals(2, disputedList.size());
        assertTrue(disputedList.contains(1l));
        assertTrue(disputedList.contains(2l));
    }

    private SettlementMessageResponse buildSettlementMessageResponse() {
        MessageMetadata metadata = new MessageMetadataBuilder().conversationID().messageID().timeStamp()
                .senderDomain("agr.usef-example.com").senderRole(USEFRole.AGR)
                .recipientDomain("brp.usef-example.com").recipientRole(USEFRole.BRP)
                .precedence(ROUTINE).build();
        SettlementMessageResponse response = new SettlementMessageResponse();
        response.setMessageMetadata(metadata);
        response.getFlexOrderSettlementStatus().addAll(buildFlexOrderSettlementStatuses());
        return response;
    }

    private List<FlexOrderSettlementStatus> buildFlexOrderSettlementStatuses() {
        FlexOrderSettlementStatus foss1 = new FlexOrderSettlementStatus();
        FlexOrderSettlementStatus foss2 = new FlexOrderSettlementStatus();
        foss1.setDisposition(DispositionAcceptedDisputed.ACCEPTED);
        foss1.setOrderReference("1");
        foss2.setDisposition(DispositionAcceptedDisputed.DISPUTED);
        foss2.setOrderReference("2");
        return Arrays.asList(foss1, foss2);
    }

    private Message buildSavedSettlementMessage() throws IOException {
        Message message = new Message();
        StringWriter writer = new StringWriter();
        IOUtils.copy(
                Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("energy/usef/brp/controller/settlement-message-test.xml"), writer);
        message.setXml(writer.toString());
        return message;
    }
}
