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

package energy.usef.dso.workflow.validate.create.flexorder;

import static energy.usef.dso.workflow.validate.create.flexorder.PlaceFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST;
import static java.util.stream.Collectors.groupingBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexOrder;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.model.GridSafetyAnalysis;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.validate.create.flexrequest.DsoCreateFlexRequestCoordinator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link DsoCreateFlexRequestCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class DsoFlexOrderCoordinatorTest {

    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678";
    private static final LocalDate PTU_DATE = new LocalDate(2014, 11, 28);

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;
    @Mock
    private WorkflowStepExecuter workflowStubLoader;
    @Mock
    private Config config;
    @Mock
    private JMSHelperService jmsHelperService;

    private DsoFlexOrderCoordinator coordinator;
    private SequenceGeneratorService sequenceGeneratorService;

    @Before
    public void init() {
        coordinator = new DsoFlexOrderCoordinator();
        sequenceGeneratorService = new SequenceGeneratorService();
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, dsoPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, workflowStubLoader);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);

        PowerMockito.when(config.getProperty(ConfigParam.PTU_DURATION)).thenReturn("15");
        PowerMockito.when(config.getProperty(ConfigParam.TIME_ZONE)).thenReturn("Europe/Amsterdam");

        PowerMockito.when(dsoPlanboardBusinessService
                .findLatestGridSafetyAnalysisWithDispositionRequested(Matchers.eq(CONGESTION_POINT_ENTITY_ADDRESS),
                        Matchers.eq(PTU_DATE))).thenReturn(buildGridSafetyAnalysis());
        PowerMockito.when(corePlanboardBusinessService
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(PrognosisType.D_PROGNOSIS),
                        Matchers.eq(CONGESTION_POINT_ENTITY_ADDRESS)))
                .thenReturn(buildPtuPrognosis());
        PowerMockito.when(dsoPlanboardBusinessService.findOrderableFlexOffers()).thenReturn(buildOrderableFlexOffers());
    }

    @Test
    public void testInvokeWorkflowWithOrder() {
        // mocking
        PowerMockito.when(workflowStubLoader.invoke(Matchers.any(String.class), Matchers.any(WorkflowContext.class)))
                .then(call -> {
                    WorkflowContext context = (WorkflowContext) call.getArguments()[1];
                    context.setValue(ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), Collections.singletonList(1L));
                    return context;
                });
        // invocation
        coordinator.handleEvent(new FlexOrderEvent());
        // validations
        verify(workflowStubLoader, times(1)).invoke(Matchers.eq(DsoWorkflowStep.DSO_PLACE_FLEX_ORDERS.name()), Matchers.any(WorkflowContext.class));
        verify(dsoPlanboardBusinessService, times(1)).findOrderableFlexOffers();
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(jmsHelperService, times(1)).sendMessageToOutQueue(messageCaptor.capture());
        FlexOrder flexOrder = XMLUtil.xmlToMessage(messageCaptor.getValue(), FlexOrder.class);
        Assert.assertNotNull(flexOrder);
        Assert.assertEquals(1L, flexOrder.getFlexOfferSequence());
        Assert.assertEquals("agr1.usef-example.com", flexOrder.getFlexOfferOrigin());
    }

    @Test
    public void testInvokeWorkflowWithoutOrder() {
        // mocking
        PowerMockito.when(workflowStubLoader.invoke(Matchers.any(String.class), Matchers.any(WorkflowContext.class)))
                .then(call -> {
                    WorkflowContext context = (WorkflowContext) call.getArguments()[1];
                    context.setValue(ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), new ArrayList<>());
                    return context;
                });
        // invocation
        coordinator.handleEvent(new FlexOrderEvent());
        // validations
        verify(workflowStubLoader, times(1)).invoke(Matchers.eq(DsoWorkflowStep.DSO_PLACE_FLEX_ORDERS.name()), Matchers.any(WorkflowContext.class));
        verify(dsoPlanboardBusinessService, times(1)).findOrderableFlexOffers();
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verifyNoMoreInteractions(jmsHelperService);
    }

    private Map<String, Map<LocalDate, List<PlanboardMessage>>> buildOrderableFlexOffers() {
        return buildFlexOffers().stream()
                .collect(groupingBy(flexOffer -> flexOffer.getConnectionGroup().getUsefIdentifier(),
                        groupingBy(PlanboardMessage::getPeriod)));
    }

    private List<PtuPrognosis> buildPtuPrognosis() {
        List<PtuPrognosis> result = new ArrayList<>();
        PtuPrognosis e = new PtuPrognosis();
        result.add(e);
        return result;
    }

    private List<GridSafetyAnalysis> buildGridSafetyAnalysis() {
        List<GridSafetyAnalysis> gsas = new ArrayList<>();
        CongestionPointConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(CONGESTION_POINT_ENTITY_ADDRESS);
        connectionGroup.setDsoDomain("dso.usef-example.com");
        for (int index = 1; index <= 96; ++index) {
            GridSafetyAnalysis gsa = new GridSafetyAnalysis();
            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setPtuDate(PTU_DATE);
            ptuContainer.setPtuIndex(index);
            gsa.setDisposition(DispositionAvailableRequested.REQUESTED);
            gsa.setPower(1000l);
            gsa.setSequence(sequenceGeneratorService.next());
            gsa.setPtuContainer(ptuContainer);
            gsa.setPrognoses(buildPrognosesForGridSafetyAnalysis());
            gsa.setConnectionGroup(connectionGroup);
            gsas.add(gsa);
        }
        return gsas;
    }

    private List<PtuPrognosis> buildPrognosesForGridSafetyAnalysis() {
        List<PtuPrognosis> prognoses = new ArrayList<>();
        PtuPrognosis p1 = new PtuPrognosis();
        p1.setSequence(sequenceGeneratorService.next());
        p1.setParticipantDomain("agr1.usef-example.com");
        PtuPrognosis p2 = new PtuPrognosis();
        p2.setSequence(sequenceGeneratorService.next());
        p2.setParticipantDomain("agr2.usef-example.com");
        prognoses.add(p1);
        prognoses.add(p2);
        return prognoses;
    }

    private List<PlanboardMessage> buildFlexOffers() {
        PlanboardMessage flexOffer = new PlanboardMessage();
        flexOffer.setPeriod(PTU_DATE);
        flexOffer.setConnectionGroup(new CongestionPointConnectionGroup(CONGESTION_POINT_ENTITY_ADDRESS));
        flexOffer.setDocumentType(DocumentType.FLEX_OFFER);
        flexOffer.setSequence(1L);
        flexOffer.setParticipantDomain("agr1.usef-example.com");
        return Collections.singletonList(flexOffer);
    }
}
