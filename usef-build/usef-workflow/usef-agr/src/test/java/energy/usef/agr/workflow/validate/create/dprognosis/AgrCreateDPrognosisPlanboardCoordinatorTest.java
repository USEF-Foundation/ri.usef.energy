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

package energy.usef.agr.workflow.validate.create.dprognosis;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.model.ForecastPowerData;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.nonudi.goals.AgrNonUdiSetAdsGoalsEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.enterprise.event.Event;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.xml.sax.SAXException;

/**
 * Test class in charge of the unit tests related to the {@link AgrCreateDPrognosisPlanboardCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class AgrCreateDPrognosisPlanboardCoordinatorTest {

    private static final LocalDate TEST_DATE = new LocalDate(2050, 11, 21);
    private static final int TEST_PTU_DURATION = 15;
    private AgrCreateDPrognosisPlanboardCoordinator coordinator;
    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private AgrPlanboardBusinessService agrPlanboardBusinessService;
    @Mock
    private CorePlanboardValidatorService planboardValidatorService;
    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Mock
    private Event<CreateDPrognosisEvent> createDPrognosisEventManager;
    @Mock
    private Event<AgrNonUdiSetAdsGoalsEvent> agrSetAdsGoalsEventManager;
    @Mock
    private Config config;
    @Mock
    private ConfigAgr configAgr;
    @Mock
    private EventValidationService eventValidationService;

    @Before
    public void init() {
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();

        coordinator = new AgrCreateDPrognosisPlanboardCoordinator();
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, planboardValidatorService);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, agrPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, "createDPrognosisEventManager", createDPrognosisEventManager);
        Whitebox.setInternalState(coordinator, configAgr);
        Whitebox.setInternalState(coordinator, "agrSetAdsGoalsEventManager", agrSetAdsGoalsEventManager);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);
        Whitebox.setInternalState(coordinator, eventValidationService);

        PowerMockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(true);
    }

    // This test gives unpredictable results. TODO: fix this.
    @Ignore
    @Test
    public void testInvokeWorkflowReturns2PTUs() throws SAXException, IOException, XpathException, BusinessValidationException {
        XMLUnit.setIgnoreWhitespace(true);

        CongestionPointConnectionGroup congestionPoint = buildCongestionPointCG();
        PowerMockito.when(corePlanboardBusinessService.findCongestionPointConnectionGroup("ean.123456789012345678"))
                .thenReturn(congestionPoint);
        PowerMockito
                .when(agrPortfolioBusinessService.findActivePortfolioForConnectionGroupLevel(Matchers.eq(TEST_DATE), Matchers.eq(
                        Optional.of(congestionPoint))))
                .thenReturn(buildPowerContainers(congestionPoint, 1));

        PowerMockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.INTRADAY_GATE_CLOSURE_PTUS))).thenReturn(0);
        // step a long PTU to be sure that we seem to be in the first PTU of the day.
        PowerMockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.PTU_DURATION))).thenReturn(TEST_PTU_DURATION);

        LocalDateTime now = new LocalDateTime();
        Integer currentPtu = 1 + DateTimeUtil.getElapsedMinutesSinceMidnight(now) / TEST_PTU_DURATION;
        // Take the first prepared PTU for this test

        coordinator.handleEvent(new CreateDPrognosisEvent(TEST_DATE, null));
        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(xmlCaptor.capture());
        String actualXml = xmlCaptor.getValue();
        XMLAssert.assertXpathEvaluatesTo("1", "count(/Prognosis)", actualXml);
        XMLAssert.assertXpathEvaluatesTo("2", "count(/Prognosis/PTU)", actualXml);
        XMLAssert.assertXpathEvaluatesTo((new Integer(currentPtu + 1)).toString(), "/Prognosis/PTU[1]/@Start", actualXml);
        XMLAssert.assertXpathEvaluatesTo("1000", "/Prognosis/PTU[1]/@Power", actualXml);
        XMLAssert.assertXpathEvaluatesTo((new Integer(currentPtu + 2)).toString(), "/Prognosis/PTU[2]/@Start", actualXml);
        XMLAssert.assertXpathEvaluatesTo("2000", "/Prognosis/PTU[2]/@Power", actualXml);

        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).storePrognosis(Matchers.eq("ean.123456789012345678"),
                Matchers.any(Prognosis.class), Matchers.eq(DocumentType.D_PROGNOSIS), Matchers.eq(DocumentStatus.SENT),
                Matchers.eq("dso.usef-example.com"), (Message) Matchers.isNull(), Matchers.eq(false));

        Mockito.verify(agrSetAdsGoalsEventManager, Mockito.times(1)).fire(Matchers.any(AgrNonUdiSetAdsGoalsEvent.class));
    }

    @Test
    public void testInvokeWorkflowDoesNotReturnPTUs() throws SAXException, IOException, XpathException, BusinessValidationException {
        XMLUnit.setIgnoreWhitespace(true);

        CongestionPointConnectionGroup congestionPoint = buildCongestionPointCG();
        PowerMockito.when(corePlanboardBusinessService.findCongestionPointConnectionGroup("ean.123456789012345678"))
                .thenReturn(congestionPoint);
        PowerMockito
                .when(agrPortfolioBusinessService.findActivePortfolioForConnectionGroupLevel(Matchers.eq(TEST_DATE), Matchers.eq(
                        Optional.of(congestionPoint))))
                .thenReturn(buildPowerContainers(congestionPoint, 1));
        PowerMockito.when(agrPlanboardBusinessService.findLastPrognoses(Matchers.any(LocalDate.class),
                Matchers.any(PrognosisType.class),
                Matchers.any(Optional.class))).then(invocation -> {
            PrognosisDto prognosisDto = new PrognosisDto();
            prognosisDto.setParticipantDomain("dso.usef-example.com");
            IntStream.rangeClosed(1, 96).mapToObj(index -> {
                PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
                ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(index));
                ptuPrognosisDto.setPower(BigInteger.valueOf(500));
                return ptuPrognosisDto;
            }).forEach(ptu -> prognosisDto.getPtus().add(ptu));
            return Collections.singletonList(prognosisDto);
        });

        PowerMockito.when(
                planboardValidatorService.isPtuContainerWithinIntradayGateClosureTime(Matchers.any(PtuContainer.class)))
                .thenReturn(true);
        PowerMockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.INTRADAY_GATE_CLOSURE_PTUS))).thenReturn(2);
        // step a long PTU to be sure that we seem to be in the first PTU of the day.
        PowerMockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.PTU_DURATION))).thenReturn(TEST_PTU_DURATION);

        coordinator.handleEvent(new CreateDPrognosisEvent(TEST_DATE, "ean.123456789012345678"));
        Mockito.verify(agrPlanboardBusinessService, Mockito.times(1))
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.any(PrognosisType.class), Matchers.any(Optional.class));
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(Mockito.anyString());
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).storePrognosis(Matchers.eq("ean.123456789012345678"),
                Matchers.any(Prognosis.class), Matchers.eq(DocumentType.D_PROGNOSIS), Matchers.eq(DocumentStatus.SENT),
                Matchers.eq("dso.usef-example.com"), (Message) Matchers.isNull(),Matchers.eq(false));

        Mockito.verify(agrSetAdsGoalsEventManager, Mockito.times(1)).fire(Matchers.any(AgrNonUdiSetAdsGoalsEvent.class));
    }

    @Test
    public void testHandleEventWithMultipleUsefIdentifiers() throws Exception {
        //
        List<CongestionPointConnectionGroup> congestionPoints = new ArrayList<>();
        CongestionPointConnectionGroup congestionPointConnectionGroup = new CongestionPointConnectionGroup();
        congestionPointConnectionGroup.setUsefIdentifier("ean.000000000");
        congestionPointConnectionGroup.setDsoDomain("dso.usef-example.com");
        congestionPoints.add(congestionPointConnectionGroup);

        CongestionPointConnectionGroup congestionPointConnectionGroup2 = new CongestionPointConnectionGroup();
        congestionPointConnectionGroup2.setUsefIdentifier("ean.000000001");
        congestionPointConnectionGroup2.setDsoDomain("dso.usef-example.com");
        congestionPoints.add(congestionPointConnectionGroup2);

        PowerMockito.when(corePlanboardBusinessService.findActiveCongestionPointAddresses(Mockito.any(LocalDate.class)))
                .thenReturn(Arrays.asList("ean.000000000", "ean.000000001"));

        // stubbing of the PlanboardMessageRepository
        PowerMockito.when(corePlanboardBusinessService.findPlanboardMessages(Matchers.eq(DocumentType.D_PROGNOSIS),
                Matchers.any(LocalDate.class), Matchers.isNull(DocumentStatus.class))).thenReturn(buildPlanboardMessages(false));

        // call the coordinator
        coordinator.handleReCreateDPrognosisEvent(buildReCreateDPrognosisEvent(DateTimeUtil.getCurrentDate()));

        // verify that an event has been sent for each PlanboardMessage
        Mockito.verify(createDPrognosisEventManager, Mockito.times(2)).fire(Matchers.any(CreateDPrognosisEvent.class));
    }

    private ReCreateDPrognosisEvent buildReCreateDPrognosisEvent(LocalDate period) {
        return new ReCreateDPrognosisEvent(period);
    }

    private List<PlanboardMessage> buildPlanboardMessages(boolean differentCongestionPoints) {
        List<PlanboardMessage> planboardMessages = new ArrayList<>();

        for (int i = 0; i < 2; ++i) {
            ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
            connectionGroup.setUsefIdentifier("ean.000000000" + (differentCongestionPoints ? "" : i + 1));
            PlanboardMessage planboardMessage = new PlanboardMessage();
            planboardMessage.setDocumentStatus(DocumentStatus.TO_BE_RECREATED);
            planboardMessage.setDocumentType(DocumentType.D_PROGNOSIS);
            planboardMessage.setConnectionGroup(connectionGroup);
            planboardMessages.add(planboardMessage);
        }
        return planboardMessages;
    }

    private CongestionPointConnectionGroup buildCongestionPointCG() {
        CongestionPointConnectionGroup congestionPointConnectionGroup = new CongestionPointConnectionGroup();
        congestionPointConnectionGroup.setUsefIdentifier("ean.123456789012345678");
        congestionPointConnectionGroup.setDsoDomain("dso.usef-example.com");
        return congestionPointConnectionGroup;
    }

    private Map<ConnectionGroup, Map<Integer, PowerContainer>> buildPowerContainers(ConnectionGroup connectionGroup,
            int currentPtu) {

        PowerContainer cf1 = new PowerContainer();
        cf1.setTimeIndex(currentPtu + 1);
        cf1.setForecast(new ForecastPowerData());
        cf1.getForecast().setAverageConsumption(BigInteger.valueOf(-1000));

        PowerContainer cf2 = new PowerContainer();
        cf2.setTimeIndex(currentPtu + 1);
        cf2.setForecast(new ForecastPowerData());
        cf2.getForecast().setAverageConsumption(BigInteger.valueOf(-2000));

        Map<ConnectionGroup, Map<Integer, PowerContainer>> result = new HashMap<>();

        result.put(connectionGroup, new HashMap<>());
        result.get(connectionGroup).put(1, cf1);
        result.get(connectionGroup).put(2, cf2);
        return result;
    }

}
