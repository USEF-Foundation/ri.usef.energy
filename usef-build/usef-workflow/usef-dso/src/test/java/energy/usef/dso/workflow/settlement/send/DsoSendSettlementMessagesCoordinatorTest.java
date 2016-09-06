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

package energy.usef.dso.workflow.settlement.send;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.when;

import energy.usef.core.config.Config;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.FlexOrderSettlement;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuSettlement;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.workflow.settlement.CoreSettlementBusinessService;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.model.AggregatorOnConnectionGroupState;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class in charge of the unit tests related to the {@link DsoSendSettlementMessagesCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class DsoSendSettlementMessagesCoordinatorTest {

    private static final String AGR_DOMAIN = "agr1.usef-example.com";
    private static final String CONGESTION_POINT = "ean.111111111111";
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoSendSettlementMessagesCoordinatorTest.class);
    @Mock
    private Config config;
    @Mock
    private ConfigDso configDso;
    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private CoreSettlementBusinessService coreSettlementBusinessService;

    private DsoSendSettlementMessagesCoordinator coordinator;

    @Before
    public void init() {
        coordinator = new DsoSendSettlementMessagesCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configDso);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, dsoPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, coreSettlementBusinessService);
        Mockito.when(configDso.getIntegerProperty(ConfigDsoParam.DSO_SETTLEMENT_RESPONSE_WAITING_DURATION)).thenReturn(4);
    }

    @Test
    public void testInvokeWorkflowIsSuccessful() {
        when(dsoPlanboardBusinessService.findAggregatorsWithOverlappingActivityForPeriod(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(buildAggregatorsList());
        when(coreSettlementBusinessService.findFlexOrderSettlementsForPeriod(any(LocalDate.class), any(LocalDate.class),
                eq(Optional.<String>empty()), eq(Optional.<String>empty()))).thenReturn(buildFlexOrderSettlementList());

        coordinator.invokeWorkflow(new SendSettlementMessageEvent(2015, DateTimeConstants.APRIL));
        Mockito.verify(dsoPlanboardBusinessService, Mockito.times(1)).findAggregatorsWithOverlappingActivityForPeriod(
                eq(new LocalDate(2015, 4, 1)), eq(new LocalDate(2015, 4, 30)));
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(messageCaptor.capture());
        LOGGER.debug(messageCaptor.getValue());
    }

    @Test
    public void testInvokeWorkflowDefaultSettlement() {
        when(dsoPlanboardBusinessService
                .findAggregatorsWithOverlappingActivityForPeriod(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(buildAggregatorsList());
        when(coreSettlementBusinessService
                .findFlexOrderSettlementsForPeriod(any(LocalDate.class), any(LocalDate.class), eq(Optional.<String>empty()),
                        eq(Optional.<String>empty()))).thenReturn(new ArrayList<>());

        coordinator.invokeWorkflow(new SendSettlementMessageEvent(2015, DateTimeConstants.APRIL));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(messageCaptor.capture());
        LOGGER.debug(messageCaptor.getValue());
    }

    private List<AggregatorOnConnectionGroupState> buildAggregatorsList() {
        CongestionPointConnectionGroup connectionGroup = new CongestionPointConnectionGroup(CONGESTION_POINT);
        Aggregator aggregator = new Aggregator();
        aggregator.setDomain(AGR_DOMAIN);
        AggregatorOnConnectionGroupState state = new AggregatorOnConnectionGroupState();
        state.setAggregator(aggregator);
        state.setCongestionPointConnectionGroup(connectionGroup);
        return Collections.singletonList(state);
    }

    private List<FlexOrderSettlement> buildFlexOrderSettlementList() {
        LocalDate period = new LocalDate(2015, 10, 21);
        PlanboardMessage flexOrder = new PlanboardMessage();
        flexOrder.setSequence(123456789L);
        flexOrder.setParticipantDomain(AGR_DOMAIN);
        flexOrder.setConnectionGroup(new CongestionPointConnectionGroup(CONGESTION_POINT));
        FlexOrderSettlement flexOrderSettlement = new FlexOrderSettlement();
        flexOrderSettlement.setPeriod(period);
        flexOrderSettlement.setConnectionGroup(new CongestionPointConnectionGroup(CONGESTION_POINT));
        flexOrderSettlement.setFlexOrder(flexOrder);
        IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuSettlement ptuSettlement = new PtuSettlement();
            ptuSettlement.setActualPower(BigInteger.TEN);
            ptuSettlement.setDeliveredFlexPower(BigInteger.TEN);
            ptuSettlement.setPtuContainer(new PtuContainer(period, index));
            ptuSettlement.setPowerDeficiency(BigInteger.TEN);
            ptuSettlement.setPrognosisPower(BigInteger.TEN);
            ptuSettlement.setPenalty(BigDecimal.TEN);
            ptuSettlement.setPrice(BigDecimal.TEN);
            return ptuSettlement;
        }).forEach(ptuSettlement -> flexOrderSettlement.getPtuSettlements().add(ptuSettlement));
        return Collections.singletonList(flexOrderSettlement);
    }

}
