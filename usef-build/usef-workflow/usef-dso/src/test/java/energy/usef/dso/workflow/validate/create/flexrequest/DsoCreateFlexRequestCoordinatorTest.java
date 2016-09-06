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

package energy.usef.dso.workflow.validate.create.flexrequest;

import static energy.usef.dso.workflow.DsoWorkflowStep.DSO_CREATE_FLEX_REQUEST;

import energy.usef.core.config.Config;
import energy.usef.core.data.xml.bean.message.FlexRequest;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PtuFlexRequestDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.model.GridSafetyAnalysis;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestStepParameter.OUT;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

/**
 * Test class in charge of the unit tests related to the {@link DsoCreateFlexRequestCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class DsoCreateFlexRequestCoordinatorTest {

    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678";
    private static final LocalDate EXPIRED_PTU_DATE = new LocalDate(2010, 11, 28);
    private static final LocalDate PTU_DATE = new LocalDate(2030, 11, 28);

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private DsoPlanboardBusinessService planboardBusinessService;
    @Mock
    private WorkflowStepExecuter workflowStubLoader;
    @Mock
    private Config config;
    @Mock
    private ConfigDso configDso;
    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private EventValidationService eventValidationService;

    private SequenceGeneratorService sequenceGeneratorService;

    private DsoCreateFlexRequestCoordinator coordinator;

    @Before
    public void init() {
        coordinator = new DsoCreateFlexRequestCoordinator();
        sequenceGeneratorService = new SequenceGeneratorService();
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, planboardBusinessService);
        Whitebox.setInternalState(coordinator, workflowStubLoader);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configDso);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);
        Whitebox.setInternalState(coordinator, eventValidationService);

        PowerMockito.when(workflowStubLoader.invoke(Mockito.eq(DSO_CREATE_FLEX_REQUEST.name()),
                Mockito.any(WorkflowContext.class)))
                .then(call -> {
                    WorkflowContext context = (WorkflowContext) call.getArguments()[1];
                    context.setValue(OUT.FLEX_REQUESTS_DTO_LIST.name(), buildFlexRequestDto());
                    return context;
                });

        PowerMockito.when(planboardBusinessService
                .findLatestGridSafetyAnalysisWithDispositionRequested(Matchers.eq(CONGESTION_POINT_ENTITY_ADDRESS),
                        Matchers.eq(PTU_DATE))).thenReturn(buildGridSafetyAnalysis());
        PowerMockito.when(
                planboardBusinessService.getAggregatorsByCongestionPointAddress(Matchers.any(String.class),
                        Matchers.any(LocalDate.class))).thenReturn(buildAggregatorList());
    }

    @Test
    public void testInvokeWorkflow() throws BusinessValidationException {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        coordinator.createFlexRequests(new CreateFlexRequestEvent(CONGESTION_POINT_ENTITY_ADDRESS, PTU_DATE, new Integer[] {}));
        Mockito.verify(planboardBusinessService, Mockito.times(1))
                .findLatestGridSafetyAnalysisWithDispositionRequested(Matchers.eq(CONGESTION_POINT_ENTITY_ADDRESS),
                        Matchers.eq(PTU_DATE));
        Mockito.verify(workflowStubLoader, Mockito.times(1)).invoke(Mockito.eq(DSO_CREATE_FLEX_REQUEST.name()),
                Mockito.any(WorkflowContext.class));
        Mockito.verify(planboardBusinessService, Mockito.times(1))
                .getAggregatorsByCongestionPointAddress(Matchers.any(String.class), Matchers.any(LocalDate.class));

        Mockito.verify(jmsHelperService, Mockito.times(2)).sendMessageToOutQueue(messageCaptor.capture());
        Mockito.verify(corePlanboardBusinessService, Mockito.times(2))
                .storeFlexRequest(Mockito.anyString(), Matchers.any(FlexRequest.class), Matchers.eq(DocumentStatus.SENT),
                        Matchers.anyString());

        List<String> messages = messageCaptor.getAllValues();
        Assert.assertTrue(messages.get(0).contains("agr1.usef-example.com"));
        Assert.assertTrue(messages.get(1).contains("agr2.usef-example.com"));
    }

    @Test
    public void testInvokeWorkflowExpired() throws BusinessValidationException {
        coordinator.createFlexRequests(new CreateFlexRequestEvent(CONGESTION_POINT_ENTITY_ADDRESS, EXPIRED_PTU_DATE, new Integer[] {}));
        Mockito.verify(eventValidationService, Mockito.times(1));
    }

    private List<GridSafetyAnalysis> buildGridSafetyAnalysis() {
        List<GridSafetyAnalysis> gsas = new ArrayList<>();
        CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setUsefIdentifier(CONGESTION_POINT_ENTITY_ADDRESS);
        congestionPoint.setDsoDomain("dso.usef-example.com");
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
            gsa.setConnectionGroup(congestionPoint);
            gsas.add(gsa);
        }
        return gsas;
    }

    private List<PtuPrognosis> buildPrognosesForGridSafetyAnalysis() {
        List<PtuPrognosis> prognoses = new ArrayList<>();
        PtuPrognosis p1 = new PtuPrognosis();
        p1.setSequence(1L);
        p1.setParticipantDomain("agr1.usef-example.com");
        PtuPrognosis p2 = new PtuPrognosis();
        p2.setSequence(2L);
        p2.setParticipantDomain("agr2.usef-example.com");
        prognoses.add(p1);
        prognoses.add(p2);
        return prognoses;
    }

    private List<FlexRequestDto> buildFlexRequestDto() {
        FlexRequestDto flexRequestDto = new FlexRequestDto();
        flexRequestDto.setConnectionGroupEntityAddress("ean.123456789012345678");
        flexRequestDto.setPeriod(PTU_DATE);
        flexRequestDto.setPrognosisSequenceNumber(1L);
        flexRequestDto.setSequenceNumber(sequenceGeneratorService.next());

        PtuFlexRequestDto ptuFlexRequestDto = new PtuFlexRequestDto();
        ptuFlexRequestDto.setDisposition(DispositionTypeDto.REQUESTED);
        ptuFlexRequestDto.setPower(BigInteger.valueOf(2000));
        ptuFlexRequestDto.setPtuIndex(BigInteger.ONE);

        flexRequestDto.getPtus().add(ptuFlexRequestDto);
        List<FlexRequestDto> flexRequestDtos = new ArrayList<>();
        flexRequestDtos.add(flexRequestDto);
        return flexRequestDtos;
    }

    private List<Aggregator> buildAggregatorList() {
        Aggregator agr1 = new Aggregator();
        Aggregator agr2 = new Aggregator();
        agr1.setDomain("agr1.usef-example.com");
        agr2.setDomain("agr2.usef-example.com");
        return Arrays.asList(agr1, agr2);
    }
}
