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

package energy.usef.agr.workflow.plan.create.aplan;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.model.ForecastPowerData;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.nonudi.goals.AgrNonUdiSetAdsGoalsEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.DateTimeUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.event.Event;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.joda.time.LocalDate;
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

/**
 * Test class in charge of the unit tests related to the {@link AgrCreateAPlanPlanboardCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class AgrCreateAPlanPlanboardCoordinatorTest {
    private static final LocalDate TEST_DATE = new LocalDate(2050, 11, 21);
    private static final int TEST_PTU_DURATION = 15;
    private AgrCreateAPlanPlanboardCoordinator coordinator;
    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private CorePlanboardValidatorService planboardValidatorService;
    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Mock
    private Config config;
    @Mock
    private ConfigAgr configAgr;
    @Mock
    private Event<AgrNonUdiSetAdsGoalsEvent> agrSetAdsGoalsEventManager;
    @Mock
    private EventValidationService eventValidationService;

    @Before
    public void init() throws Exception {
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();

        coordinator = new AgrCreateAPlanPlanboardCoordinator();
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, planboardValidatorService);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configAgr);
        Whitebox.setInternalState(coordinator, "agrSetAdsGoalsEventManager", agrSetAdsGoalsEventManager);
        Whitebox.setInternalState(coordinator, eventValidationService);
    }

    @Test
    public void testHandleEvent() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);

        BrpConnectionGroup connectionGroup = buildBrpConnectionGroup();

        PowerMockito.when(corePlanboardBusinessService.findConnectionGroup(null))
                .thenReturn(connectionGroup);
        PowerMockito.when(
                agrPortfolioBusinessService.findActivePortfolioForConnectionGroupLevel(Matchers.eq(TEST_DATE),
                        Matchers.eq(Optional.of(connectionGroup))))
                .thenReturn(buildPowerContainers(connectionGroup, 1));
        PowerMockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.INTRADAY_GATE_CLOSURE_PTUS))).thenReturn(0);
        PowerMockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.PTU_DURATION))).thenReturn(TEST_PTU_DURATION);
        PowerMockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(true);

        coordinator.handleEvent(new CreateAPlanEvent(TEST_DATE, null));

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(xmlCaptor.capture());
        String actualXml = xmlCaptor.getValue();
        XMLAssert.assertXpathEvaluatesTo("1", "count(/Prognosis)", actualXml);
        XMLAssert.assertXpathEvaluatesTo("3", "count(/Prognosis/PTU)", actualXml);

        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .storePrognosis(Matchers.any(Prognosis.class), Matchers.any(ConnectionGroup.class),
                        Matchers.any(DocumentType.class), Matchers.any(DocumentStatus.class), Matchers.any(String.class),
                        (Message) Matchers.isNull(),
                        Matchers.eq(false));

        Mockito.verify(agrSetAdsGoalsEventManager, Mockito.times(1)).fire(Matchers.any(AgrNonUdiSetAdsGoalsEvent.class));
    }

    private List<PtuPrognosis> buildAPlans() {
        return IntStream.rangeClosed(1, 96).mapToObj(i -> newAPlan(i, 10l, "brp.usef-example.com")).collect(Collectors.toList());
    }

    private PtuPrognosis newAPlan(Integer ptuIndex, Long sequence, String domain) {
        ConnectionGroup connectionGroup = new BrpConnectionGroup();
        connectionGroup.setUsefIdentifier(domain);
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuDate(DateTimeUtil.getCurrentDate());
        ptuContainer.setPtuIndex(ptuIndex);

        PtuPrognosis ptuPrognosis = new PtuPrognosis();
        ptuPrognosis.setConnectionGroup(connectionGroup);
        ptuPrognosis.setPtuContainer(ptuContainer);
        ptuPrognosis.setSequence(sequence);
        ptuPrognosis.setParticipantDomain(domain);
        ptuPrognosis.setType(PrognosisType.A_PLAN);
        ptuPrognosis.setPower(BigInteger.valueOf(500));
        return ptuPrognosis;
    }

    private Map<ConnectionGroup, Map<Integer, PowerContainer>> buildPowerContainers(BrpConnectionGroup connectionGroup,
            int currentPtu) {

        PowerContainer cf1 = new PowerContainer();
        cf1.setTimeIndex(currentPtu + 1);
        cf1.setForecast(new ForecastPowerData());
        cf1.getForecast().setAverageConsumption(BigInteger.valueOf(-1000));

        PowerContainer cf2 = new PowerContainer();
        cf2.setTimeIndex(currentPtu + 1);
        cf2.setForecast(new ForecastPowerData());
        cf2.getForecast().setAverageConsumption(BigInteger.valueOf(-2000));

        PowerContainer cf3 = new PowerContainer();
        cf3.setTimeIndex(currentPtu + 1);
        cf3.setForecast(new ForecastPowerData());
        cf3.getForecast().setAverageConsumption(BigInteger.valueOf(-4000));

        Map<ConnectionGroup, Map<Integer, PowerContainer>> result = new HashMap<>();

        result.put(connectionGroup, new HashMap<>());
        result.get(connectionGroup).put(1, cf1);
        result.get(connectionGroup).put(2, cf2);
        result.get(connectionGroup).put(3, cf3);
        return result;
    }

    private BrpConnectionGroup buildBrpConnectionGroup() {
        BrpConnectionGroup connectionGroup = new BrpConnectionGroup();
        connectionGroup.setUsefIdentifier("brp.usef-example.com");
        connectionGroup.setBrpDomain("brp.usef-example.com");
        return connectionGroup;
    }

}
