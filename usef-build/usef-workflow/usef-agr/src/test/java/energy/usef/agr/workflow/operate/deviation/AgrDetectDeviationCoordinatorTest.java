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

package energy.usef.agr.workflow.operate.deviation;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_DETECT_DEVIATION_FROM_PROGNOSES;
import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_NON_UDI_DETECT_DEVIATION_FROM_PROGNOSES;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.util.ReflectionUtil;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

/**
 * Test class in charge of the unit tests related to the {@link AgrDetectDeviationCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class AgrDetectDeviationCoordinatorTest {
    public static final String CG_ENTITY_ADRESS = "ean.23423425";
    public static final String BRP_ENTITY_ADRESS = "brp.usef-example.com";
    private AgrDetectDeviationCoordinator coordinator;

    @Mock
    private Logger LOGGER;

    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private Event<ReOptimizePortfolioEvent> eventManager;

    @Mock
    private Config config;

    @Mock
    private ConfigAgr configAgr;

    @Mock
    private EventValidationService eventValidationService;

    @Before
    public void init() throws Exception {
        coordinator = new AgrDetectDeviationCoordinator();
        ReflectionUtil.setFinalStatic(AgrDetectDeviationCoordinator.class.getDeclaredField("LOGGER"), LOGGER);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configAgr);
        Whitebox.setInternalState(coordinator, eventManager);
        Whitebox.setInternalState(coordinator, eventValidationService);
        Mockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);

        Mockito.when(corePlanboardBusinessService
                .findSinglePlanboardMessage(Mockito.anyLong(), Mockito.any(DocumentType.class), Mockito.anyString()))
                .thenReturn(new PlanboardMessage());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWorkflow() throws BusinessValidationException {
        Mockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(false);
        WorkflowContext workflowContextIn = new DefaultWorkflowContext();
        workflowContextIn.setValue(DetectDeviationFromPrognosisStepParameter.OUT.DEVIATION_INDEX_LIST.name(),
                new ArrayList<Integer>());
        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_DETECT_DEVIATION_FROM_PROGNOSES.name()), Mockito.any()))
                .thenReturn(workflowContextIn);

        List<ConnectionPortfolioDto> connectionPortfolioDtos = buildConnections(CG_ENTITY_ADRESS);
        connectionPortfolioDtos.addAll(buildConnections(BRP_ENTITY_ADRESS));

        Mockito.when(corePlanboardBusinessService.findLastPrognoses(Matchers.any(LocalDate.class), Matchers.any(String.class)))
                .then(call -> buildPrognosisList((String) call.getArguments()[1]));
        Mockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(Matchers.any(LocalDate.class)))
                .thenReturn(connectionPortfolioDtos);
        Mockito.when(corePlanboardBusinessService
                .buildConnectionGroupsToConnectionsMap(Matchers.any(LocalDate.class)))
                .thenReturn(buildConnectionPortfolioMap());

        coordinator.handleEvent(new DetectDeviationEvent(DateTimeUtil.getCurrentDate()));

        verify(workflowStepExecuter, times(2)).invoke(Matchers.eq(AgrWorkflowStep.AGR_DETECT_DEVIATION_FROM_PROGNOSES.name()),
                Matchers.any(WorkflowContext.class));
        verify(corePlanboardBusinessService, times(1))
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(CG_ENTITY_ADRESS));
        verify(corePlanboardBusinessService, times(1))
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(BRP_ENTITY_ADRESS));
        verify(agrPortfolioBusinessService, times(1))
                .findConnectionPortfolioDto(Matchers.any(LocalDate.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWorkflowForNonUdiAggregator() throws BusinessValidationException {
        Mockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(true);
        WorkflowContext workflowContextIn = new DefaultWorkflowContext();
        workflowContextIn.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.OUT.DEVIATION_INDEX_LIST.name(),
                new ArrayList<Integer>());
        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_NON_UDI_DETECT_DEVIATION_FROM_PROGNOSES.name()), Mockito.any()))
                .thenReturn(workflowContextIn);

        List<ConnectionPortfolioDto> connectionPortfolioDtos = buildConnections(CG_ENTITY_ADRESS);
        connectionPortfolioDtos.addAll(buildConnections(BRP_ENTITY_ADRESS));

        Mockito.when(corePlanboardBusinessService.findLastPrognoses(Matchers.any(LocalDate.class), Matchers.any(String.class)))
                .then(call -> buildPrognosisList((String) call.getArguments()[1]));
        Mockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(Matchers.any(LocalDate.class)))
                .thenReturn(connectionPortfolioDtos);
        Mockito.when(corePlanboardBusinessService
                .buildConnectionGroupsToConnectionsMap(Matchers.any(LocalDate.class)))
                .thenReturn(buildConnectionPortfolioMap());

        coordinator.handleEvent(new DetectDeviationEvent(DateTimeUtil.getCurrentDate()));

        verify(workflowStepExecuter, times(2)).invoke(Matchers.eq(AgrWorkflowStep.AGR_NON_UDI_DETECT_DEVIATION_FROM_PROGNOSES.name()),
                Matchers.any(WorkflowContext.class));
        verify(corePlanboardBusinessService, times(1))
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(CG_ENTITY_ADRESS));
        verify(corePlanboardBusinessService, times(1))
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(BRP_ENTITY_ADRESS));
        verify(agrPortfolioBusinessService, times(1))
                .findConnectionPortfolioDto(Matchers.any(LocalDate.class));
    }

    private Map<String, List<String>> buildConnectionPortfolioMap() {
        Map<String, List<String>> connectionGroupToConnectionsMap = new HashMap<>();
        connectionGroupToConnectionsMap.put(CG_ENTITY_ADRESS, Arrays.asList(CG_ENTITY_ADRESS));
        connectionGroupToConnectionsMap.put(BRP_ENTITY_ADRESS, Arrays.asList(BRP_ENTITY_ADRESS));
        return connectionGroupToConnectionsMap;
    }

    private List<ConnectionPortfolioDto> buildConnections(String usefIdentifier) {
        List<ConnectionPortfolioDto> connectionPortfolioDTOs = new ArrayList<>();

        ConnectionPortfolioDto connectionPortfolioDTO = new ConnectionPortfolioDto(usefIdentifier);
        LocalDate today = DateTimeUtil.getCurrentDate();

        for (int ptuIndex = 1; ptuIndex <= 96; ptuIndex++) {
            PowerContainerDto powerContainer = new PowerContainerDto(today, ptuIndex);
            powerContainer.getForecast().setUncontrolledLoad(BigInteger.TEN);
            connectionPortfolioDTO.getConnectionPowerPerPTU().put(ptuIndex, powerContainer);
        }
        connectionPortfolioDTOs.add(connectionPortfolioDTO);

        return connectionPortfolioDTOs;
    }

    private List<PtuPrognosis> buildPrognosisList(String usefIdentifier) {
        List<PtuPrognosis> prognoses = new ArrayList<>();
        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup(usefIdentifier);
        for (int i = 1; i <= 12; ++i) {
            PtuPrognosis prognosis = new PtuPrognosis();
            prognosis.setPtuContainer(new PtuContainer(DateTimeUtil.getCurrentDate(), i));
            prognosis.setConnectionGroup(connectionGroup);
            prognosis.setPower(BigInteger.valueOf(i));
            prognoses.add(prognosis);
        }
        return prognoses;
    }

}
