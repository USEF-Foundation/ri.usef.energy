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

package energy.usef.agr.workflow.operate.recreate.prognoses;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_RECREATE_PROGNOSES;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ForecastPowerDataDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesWorkflowParameter.OUT;
import energy.usef.agr.workflow.plan.create.aplan.CreateAPlanEvent;
import energy.usef.agr.workflow.plan.recreate.aplan.ReCreateAPlanEvent;
import energy.usef.agr.workflow.validate.create.dprognosis.ReCreateDPrognosisEvent;
import energy.usef.core.config.Config;
import energy.usef.core.event.RequestMoveToValidateEvent;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PrognosisTypeDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

/**
 * Test class in charge of the unit tests related to the {@link AgrReCreatePrognosesCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrReCreatePrognosesCoordinatorTest {

    private AgrReCreatePrognosesCoordinator coordinator;

    @Mock
    private AgrPlanboardBusinessService agrPlanboardBusinessService;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private Event<ReCreateAPlanEvent> reCreateAPlanEventManager;
    @Mock
    private Event<CreateAPlanEvent> createAPlanEventManager;
    @Mock
    private Event<ReCreateDPrognosisEvent> reCreateDPrognosisEventManager;
    @Mock
    private Event<RequestMoveToValidateEvent> moveToValidateEventManager;
    @Mock
    private EventValidationService eventValidationService;
    @Mock
    private Config config;

    @Before
    public void setUp() {
        coordinator = new AgrReCreatePrognosesCoordinator();
        Whitebox.setInternalState(coordinator, agrPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, eventValidationService);

        Whitebox.setInternalState(coordinator, "reCreateAPlanEventManager", reCreateAPlanEventManager);
        Whitebox.setInternalState(coordinator, "createAPlanEventManager", createAPlanEventManager);
        Whitebox.setInternalState(coordinator, "reCreateDPrognosisEventManager", reCreateDPrognosisEventManager);
        Whitebox.setInternalState(coordinator, "moveToValidateEventManager", moveToValidateEventManager);

    }

    @Test
    public void testHandleEventIsSuccessful() throws BusinessValidationException {
        Long[] dprognosisSequences = new Long[] { 3l, 4l, 5l };

        List<PrognosisDto> latestDPrognoses = buildLatestDPrognoses(dprognosisSequences);
        PowerMockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_RECREATE_PROGNOSES.name()), Mockito.any()))
                .thenReturn(buildContextAfterPBC());
        PowerMockito.when(
                agrPlanboardBusinessService.findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(PrognosisType.A_PLAN),
                        Matchers.eq(Optional.empty()))).thenReturn(buildLatestAPlans(1l, 2l));
        PowerMockito.when(
                agrPlanboardBusinessService.findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(PrognosisType.D_PROGNOSIS),
                        Matchers.eq(Optional.empty()))).thenReturn(latestDPrognoses);
        PowerMockito.when(corePlanboardBusinessService.findActiveConnectionGroupsWithConnections(Matchers.any(LocalDate.class)))
                .thenReturn(buildConnectionGroupToConnections());
        PowerMockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(Matchers.any(LocalDate.class)))
                .thenReturn(buildConnectionPortfolio());

        coordinator.handleEvent(buildReCreatePrognosesEvent(DateTimeUtil.getCurrentDate()));

        ArgumentCaptor<ReCreateDPrognosisEvent> reCreateDPrognosisEventCaptor = ArgumentCaptor.forClass(
                ReCreateDPrognosisEvent.class);
        verify(agrPlanboardBusinessService, times(1)).findLastPrognoses(Matchers.any(LocalDate.class),
                Matchers.eq(PrognosisType.A_PLAN),
                Matchers.any(Optional.class));
        verify(agrPlanboardBusinessService, times(1)).findLastPrognoses(Matchers.any(LocalDate.class),
                Matchers.eq(PrognosisType.D_PROGNOSIS), Matchers.any(Optional.class));
        verify(reCreateDPrognosisEventManager, times(1)).fire(reCreateDPrognosisEventCaptor.capture());

        ReCreateDPrognosisEvent capturedReCreateDPrognosisEvent = reCreateDPrognosisEventCaptor.getValue();
        Assert.assertNotNull(capturedReCreateDPrognosisEvent);
        Assert.assertEquals(DateTimeUtil.getCurrentDate(), capturedReCreateDPrognosisEvent.getPeriod());
    }

    @Test
    public void testHandleEventIsSuccessfulForPlanPhase() throws BusinessValidationException {
        ArgumentCaptor<WorkflowContext> contextCaptor = ArgumentCaptor.forClass(WorkflowContext.class);
        Long[] dprognosisSequences = new Long[] { 3l, 4l, 5l };
        Long[] aplansSequences = new Long[] { 1l, 2l };

        List<PrognosisDto> latestDPrognoses = buildLatestDPrognoses(dprognosisSequences);
        PowerMockito
                .when(workflowStepExecuter.invoke(Mockito.eq(AGR_RECREATE_PROGNOSES.name()), contextCaptor.capture()))
                .thenReturn(buildContextAfterPBC());
        PowerMockito.when(
                agrPlanboardBusinessService.findLastPrognoses(Matchers.any(LocalDate.class),
                        Matchers.eq(PrognosisType.A_PLAN),
                        Matchers.any(Optional.class))).thenReturn(buildLatestAPlans(aplansSequences));
        PowerMockito.when(
                agrPlanboardBusinessService.findLastPrognoses(Matchers.any(LocalDate.class),
                        Matchers.eq(PrognosisType.D_PROGNOSIS),
                        Matchers.any(Optional.class))).thenReturn(latestDPrognoses);

        coordinator.handleEvent(buildReCreatePrognosesEvent(DateTimeUtil.getCurrentDate().plusDays(1)));

        verify(agrPlanboardBusinessService, times(1)).findLastPrognoses(Matchers.any(LocalDate.class),
                Matchers.eq(PrognosisType.A_PLAN),
                Matchers.any(Optional.class));
        verify(agrPlanboardBusinessService, times(1)).findLastPrognoses(Matchers.any(LocalDate.class),
                Matchers.eq(PrognosisType.D_PROGNOSIS), Matchers.any(Optional.class));
        verify(reCreateDPrognosisEventManager, times(1)).fire(Matchers.any(ReCreateDPrognosisEvent.class));
        verify(reCreateAPlanEventManager, times(1)).fire(Matchers.any(ReCreateAPlanEvent.class));
        verify(corePlanboardBusinessService, times(aplansSequences.length)).findSinglePlanboardMessage(Matchers.any(Long.class),
                Matchers.eq(DocumentType.A_PLAN), Matchers.eq("brp.usef-example.com"));
        verify(corePlanboardBusinessService, times(dprognosisSequences.length)).findSinglePlanboardMessage(Matchers.any(Long.class),
                Matchers.eq(DocumentType.D_PROGNOSIS), Matchers.eq("dso.usef-example.com"));
    }

    private WorkflowContext buildContextAfterPBC() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(OUT.REQUIRES_NEW_A_PLAN_SEQUENCES_LIST.name(), Arrays.asList(1l, 2l));
        context.setValue(OUT.REQUIRES_NEW_D_PROGNOSIS_SEQUENCES_LIST.name(), Arrays.asList(3l, 4l, 5l));
        return context;
    }

    private Map<ConnectionGroup, List<Connection>> buildConnectionGroupToConnections() {
        final ConnectionGroup congestionPoint = new CongestionPointConnectionGroup("ean.123456789012345678");
        final ConnectionGroup brpConnectionGroup = new BrpConnectionGroup("brp.usef-example.com");
        final Connection connection1 = new Connection("ean.000000000001");
        final Connection connection2 = new Connection("ean.000000000002");
        final Connection connection3 = new Connection("ean.000000000003");
        Map<ConnectionGroup, List<Connection>> result = new HashMap<>();
        result.put(congestionPoint, Arrays.asList(connection1, connection2));
        result.put(brpConnectionGroup, Arrays.asList(connection2, connection3));
        return result;
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolio() {
        ConnectionPortfolioDto connectionDto1 = new ConnectionPortfolioDto("ean.000000000001");
        ConnectionPortfolioDto connectionDto2 = new ConnectionPortfolioDto("ean.000000000002");
        ConnectionPortfolioDto connectionDto3 = new ConnectionPortfolioDto("ean.000000000003");

        // PowerData
        ForecastPowerDataDto powerData1 = new ForecastPowerDataDto();
        powerData1.setUncontrolledLoad(BigInteger.TEN);

        ForecastPowerDataDto powerData2 = new ForecastPowerDataDto();
        powerData2.setAverageConsumption(BigInteger.TEN);

        ForecastPowerDataDto powerData3 = new ForecastPowerDataDto();
        powerData3.setAverageConsumption(BigInteger.TEN);

        // uncontrolled load
        PowerContainerDto pDto1 = new PowerContainerDto(new LocalDate(), 1);
        pDto1.setForecast(powerData1);
        PowerContainerDto pDto2 = new PowerContainerDto(new LocalDate(), 1);
        pDto2.setForecast(powerData2);
        PowerContainerDto pDto3 = new PowerContainerDto(new LocalDate(), 1);
        pDto3.setForecast(powerData3);

        connectionDto1.getConnectionPowerPerPTU().put(1, pDto1);
        connectionDto2.getConnectionPowerPerPTU().put(1, pDto2);
        connectionDto3.getConnectionPowerPerPTU().put(1, pDto3);

        return Stream.of(connectionDto1, connectionDto2, connectionDto3).collect(Collectors.toList());
    }

    private ReCreatePrognosesEvent buildReCreatePrognosesEvent(LocalDate period) {
        return new ReCreatePrognosesEvent(period);
    }

    private List<PrognosisDto> buildLatestAPlans(Long... sequences) {
        return Stream.of(sequences)
                .map(sequence -> {
                    PrognosisDto prognosisDto = new PrognosisDto();
                    prognosisDto.setParticipantDomain("brp.usef-example.com");
                    prognosisDto.setType(PrognosisTypeDto.A_PLAN);
                    prognosisDto.setPeriod(DateTimeUtil.parseDate("2015-03-03"));
                    prognosisDto.setConnectionGroupEntityAddress("brp.usef-example.com");
                    prognosisDto.setSequenceNumber(sequence);

                    prognosisDto.getPtus().addAll(
                            IntStream.rangeClosed(1, 96).mapToObj(index -> {
                                PtuPrognosisDto ptuPrognosis = new PtuPrognosisDto();
                                ptuPrognosis.setPtuIndex(BigInteger.valueOf(index));
                                ptuPrognosis.setPower(BigInteger.TEN.multiply(BigInteger.valueOf(sequence)));
                                return ptuPrognosis;
                            }).collect(Collectors.toList()));
                    return prognosisDto;
                }).collect(Collectors.toList());
    }

    private List<PrognosisDto> buildLatestDPrognoses(Long... sequences) {
        return Stream.of(sequences)
                .map(sequence -> {
                    PrognosisDto prognosisDto = new PrognosisDto();
                    prognosisDto.setParticipantDomain("dso.usef-example.com");
                    prognosisDto.setType(PrognosisTypeDto.D_PROGNOSIS);
                    prognosisDto.setPeriod(DateTimeUtil.parseDate("2015-03-03"));
                    prognosisDto.setConnectionGroupEntityAddress("ean.123456789012345678");
                    prognosisDto.setSequenceNumber(sequence);

                    prognosisDto.getPtus().addAll(
                            IntStream.rangeClosed(1, 96).mapToObj(index -> {
                                PtuPrognosisDto ptuPrognosis = new PtuPrognosisDto();
                                ptuPrognosis.setPtuIndex(BigInteger.valueOf(index));
                                ptuPrognosis.setPower(BigInteger.TEN.multiply(BigInteger.valueOf(sequence)));
                                return ptuPrognosis;
                            }).collect(Collectors.toList()));
                    return prognosisDto;
                }).collect(Collectors.toList());

    }
}
