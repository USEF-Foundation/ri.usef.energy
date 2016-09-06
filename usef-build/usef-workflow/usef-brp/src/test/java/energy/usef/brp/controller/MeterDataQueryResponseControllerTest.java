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

import energy.usef.brp.util.ReflectionUtil;
import energy.usef.brp.workflow.settlement.initiate.FinalizeInitiateSettlementEvent;
import energy.usef.core.data.xml.bean.message.ConnectionMeterData;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.data.xml.bean.message.MeterData;
import energy.usef.core.data.xml.bean.message.MeterDataQueryResponse;
import energy.usef.core.data.xml.bean.message.MeterDataSet;
import energy.usef.core.data.xml.bean.message.PTUMeterData;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.DateTimeUtil;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

/**
 * Test class in charge of the unit tests related to the {@link MeterDataQueryResponseController}.
 */
@RunWith(PowerMockRunner.class)
public class MeterDataQueryResponseControllerTest {

    @Mock
    private Logger LOGGER;

    @Mock
    private MessageService messageService;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private Event<FinalizeInitiateSettlementEvent> eventManager;

    private MeterDataQueryResponseController controller;

    @Before
    public void init() throws Exception {
        controller = new MeterDataQueryResponseController();

        ReflectionUtil.setFinalStatic(MeterDataQueryResponseController.class.getDeclaredField("LOGGER"), LOGGER);
        Whitebox.setInternalState(controller, messageService);
        Whitebox.setInternalState(controller, eventManager);
        Whitebox.setInternalState(controller, corePlanboardBusinessService);

        PowerMockito.when(corePlanboardBusinessService
                .findPlanboardMessages(Matchers.eq(DocumentType.METER_DATA_QUERY_USAGE), Matchers.any(LocalDate.class),
                        Matchers.eq(DocumentStatus.SENT)))
                .thenReturn(buildPlanboardMessageList());
        PowerMockito.when(messageService.getInitialMessageOfConversation(Matchers.any(String.class))).thenReturn(new Message());
    }

    private List<PlanboardMessage> buildPlanboardMessageList() {
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setExpirationDate(DateTimeUtil.getCurrentDateTime().plusDays(1));
        return Collections.singletonList(planboardMessage);
    }

    @Test
    public void testActionOnSuccess() throws BusinessException {
        controller.action(buildMeterDataQueryResponse(DispositionSuccessFailure.SUCCESS), null);

        ArgumentCaptor<FinalizeInitiateSettlementEvent> eventCaptor = ArgumentCaptor
                .forClass(FinalizeInitiateSettlementEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());

        Assert.assertNotNull(eventCaptor.getValue().getConnectionGroupList());
        Assert.assertFalse(eventCaptor.getValue().getConnectionGroupList().isEmpty());
    }

    @Test
    public void testActionOnFailure() throws BusinessException {
        controller.action(buildMeterDataQueryResponse(DispositionSuccessFailure.FAILURE), null);

        Mockito.verify(LOGGER, Mockito.times(1)).error(Matchers.any(String.class));
        Mockito.verify(eventManager, Mockito.times(0)).fire(Matchers.any(FinalizeInitiateSettlementEvent.class));
    }

    private MeterDataQueryResponse buildMeterDataQueryResponse(DispositionSuccessFailure success) {
        MeterDataQueryResponse meterDataQueryResponse = new MeterDataQueryResponse();
        meterDataQueryResponse.setMessageMetadata(MessageMetadataBuilder.buildDefault());
        meterDataQueryResponse.getMessageMetadata().setSenderDomain("mdc.usef-example.com");
        meterDataQueryResponse.setResult(success);

        MeterData meterData = new MeterData();
        meterData.setPeriod(new LocalDate("2010-10-01"));
        meterData.getConnectionMeterData().add(buildConnectionMeterData());

        MeterDataSet meterDataSet = new MeterDataSet();
        meterDataSet.getMeterData().add(meterData);

        meterDataQueryResponse.getMeterDataSet().add(meterDataSet);

        return meterDataQueryResponse;
    }

    private ConnectionMeterData buildConnectionMeterData() {
        ConnectionMeterData connectionMeterData = new ConnectionMeterData();
        connectionMeterData.setAGRDomain("agr1.usef-example.com");
        connectionMeterData.setEntityAddress("ean1.11111111");
        connectionMeterData.getPTUMeterData().add(buildPtuMeterData());

        return connectionMeterData;
    }

    private PTUMeterData buildPtuMeterData() {
        PTUMeterData ptuMeterData = new PTUMeterData();

        ptuMeterData.setStart(BigInteger.ONE);
        ptuMeterData.setDuration(BigInteger.ONE);
        ptuMeterData.setPower(BigInteger.TEN);

        return ptuMeterData;
    }
}
