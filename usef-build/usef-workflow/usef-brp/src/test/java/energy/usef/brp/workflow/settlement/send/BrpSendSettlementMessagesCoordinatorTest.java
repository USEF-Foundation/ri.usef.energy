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

package energy.usef.brp.workflow.settlement.send;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.AgrConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.FlexOrderSettlement;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuSettlement;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.workflow.settlement.CoreSettlementBusinessService;

/**
 * Test class in charge of the unit tests related to the {@link BrpSendSettlementMessagesCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class BrpSendSettlementMessagesCoordinatorTest {

    private static final String AGR_DOMAIN = "agr1.usef-example.com";
    private static final int YEAR = 2015;
    private static final int MONTH = 7;
    private static final int EXPIRATION_DAYS = 4;

    private BrpSendSettlementMessagesCoordinator coordinator;
    private static final Logger LOGGER = LoggerFactory.getLogger(BrpSendSettlementMessagesCoordinatorTest.class);

    @Mock
    private Config config;
    @Mock
    private ConfigBrp configBrp;
    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private CoreSettlementBusinessService coreSettlementBusinessService;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Before
    public void setUp() throws Exception {
        coordinator = new BrpSendSettlementMessagesCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();

        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configBrp);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, coreSettlementBusinessService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);

        when(configBrp.getIntegerProperty(ConfigBrpParam.BRP_SETTLEMENT_RESPONSE_WAITING_DURATION)).thenReturn(EXPIRATION_DAYS);
        when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("brp.usef-example.com");

        when(corePlanboardBusinessService.findConnectionGroupWithConnectionsWithOverlappingValidity(eq(new LocalDate(YEAR, MONTH, 1)),
                Matchers.any(LocalDate.class))).then(call -> {
            Connection connection = new Connection("ean.000000000001");
            AgrConnectionGroup agrConnectionGroup = new AgrConnectionGroup(AGR_DOMAIN);
            agrConnectionGroup.setAggregatorDomain(AGR_DOMAIN);
            return Collections.singletonMap(call.getArguments()[0], Collections.singletonMap(agrConnectionGroup, connection));
        });
    }

    @Test
    public void testInvokeWorkflowEmptyAggregators() {
        when(corePlanboardBusinessService.findConnectionGroupWithConnectionsWithOverlappingValidity(eq(new LocalDate(YEAR, MONTH, 1)),
                Matchers.any(LocalDate.class))).thenReturn(new HashMap<>());

        // actual call
        coordinator.invokeWorkflow(new SendSettlementMessageEvent(YEAR, MONTH));

        verify(jmsHelperService, times(0)).sendMessageToOutQueue(Matchers.any());
    }

    @Test
    public void testInvokeWorkflowEmptyFlexOrderSettlements() {
        // stubbing
        PowerMockito.when(coreSettlementBusinessService
                .findFlexOrderSettlementsForPeriod(any(LocalDate.class), any(LocalDate.class), eq(Optional.<String>empty()),
                        eq(Optional.<String>empty()))).thenReturn(new ArrayList<>());
        // actual call
        coordinator.invokeWorkflow(new SendSettlementMessageEvent(YEAR, MONTH));
        // verifications
        verify(configBrp, times(1)).getIntegerProperty(ConfigBrpParam.BRP_SETTLEMENT_RESPONSE_WAITING_DURATION);
        verify(corePlanboardBusinessService, times(1))
                .storeFlexOrderSettlementsPlanboardMessage(Matchers.anyListOf(FlexOrderSettlement.class), eq(EXPIRATION_DAYS),
                        eq(DocumentStatus.SENT), eq(AGR_DOMAIN), Matchers.any(Message.class));
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(jmsHelperService, times(1)).sendMessageToOutQueue(messageCaptor.capture());
        LOGGER.debug(messageCaptor.getValue());
    }

    @Test
    public void testInvokeWorkflow() {
        // stubbing
        PowerMockito
                .when(coreSettlementBusinessService.findFlexOrderSettlementsForPeriod(any(LocalDate.class), any(LocalDate.class),
                        eq(Optional.<String>empty()), eq(Optional.<String>empty()))).thenReturn(buildFlexOrderSettlementList());
        // actual call
        coordinator.invokeWorkflow(new SendSettlementMessageEvent(YEAR, MONTH));
        // verifications
        verify(configBrp, times(1)).getIntegerProperty(ConfigBrpParam.BRP_SETTLEMENT_RESPONSE_WAITING_DURATION);
        verify(corePlanboardBusinessService, times(1))
                .storeFlexOrderSettlementsPlanboardMessage(Matchers.anyListOf(FlexOrderSettlement.class), eq(EXPIRATION_DAYS),
                        eq(DocumentStatus.SENT), eq(AGR_DOMAIN), Matchers.any(Message.class));
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(jmsHelperService, times(1)).sendMessageToOutQueue(messageCaptor.capture());
        LOGGER.debug(messageCaptor.getValue());

    }

    private List<FlexOrderSettlement> buildFlexOrderSettlementList() {
        LocalDate period = new LocalDate(2015, 10, 21);
        PlanboardMessage flexOrder = new PlanboardMessage();
        flexOrder.setSequence(123456789L);
        flexOrder.setParticipantDomain(AGR_DOMAIN);
        flexOrder.setConnectionGroup(new AgrConnectionGroup(AGR_DOMAIN));
        FlexOrderSettlement flexOrderSettlement = new FlexOrderSettlement();
        flexOrderSettlement.setPeriod(period);
        flexOrderSettlement.setConnectionGroup(new AgrConnectionGroup(AGR_DOMAIN));
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
