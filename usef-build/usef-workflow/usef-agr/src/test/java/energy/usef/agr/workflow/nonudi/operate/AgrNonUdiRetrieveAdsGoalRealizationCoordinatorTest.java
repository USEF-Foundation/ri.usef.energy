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

package energy.usef.agr.workflow.nonudi.operate;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.USEFRoleDto;
import energy.usef.core.workflow.exception.WorkflowException;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.util.ArrayList;
import java.util.Collections;
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
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrNonUdiRetrieveAdsGoalRealizationCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrNonUdiRetrieveAdsGoalRealizationCoordinatorTest {

    private AgrNonUdiRetrieveAdsGoalRealizationCoordinator coordinator;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private Config config;
    @Mock
    private ConfigAgr configAgr;
    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Before
    public void setUp() throws Exception {
        coordinator = new AgrNonUdiRetrieveAdsGoalRealizationCoordinator();
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configAgr);
        Mockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.PTU_DURATION))).thenReturn(15);
        Mockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testHandleRetrieveAdsGoalRealizationEventHappyFlow() throws Exception {
        // variables and additional mocking
        final LocalDate today = DateTimeUtil.getCurrentDate();
        Mockito.when(corePlanboardBusinessService
                .findActiveConnectionGroupStates(Matchers.any(LocalDate.class), Matchers.any(Class.class)))
                .thenReturn(buildActiveConnectionGroups());
        Mockito.when(workflowStepExecuter.invoke(Matchers.eq(AgrWorkflowStep.AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION.name()),
                Matchers.any(WorkflowContext.class))).then(call -> buildOutputContext((WorkflowContext) call.getArguments()[1]));
        // invocation
        coordinator.handleRetrieveAdsGoalRealizationEvent(new AgrNonUdiRetrieveAdsGoalRealizationEvent());
        // verifications and assertions
        ArgumentCaptor<WorkflowContext> contextCaptor = ArgumentCaptor.forClass(WorkflowContext.class);
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .findActiveConnectionGroupStates(Matchers.eq(today), Matchers.isNull(Class.class));
        Mockito.verify(workflowStepExecuter, Mockito.times(1))
                .invoke(Matchers.eq(AgrWorkflowStep.AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION.name()), contextCaptor.capture());
        WorkflowContext inputContext = contextCaptor.getValue();
        Assert.assertNotNull(inputContext);
        Assert.assertEquals(today, inputContext.get(AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.PERIOD.name(), LocalDate.class));
        Assert.assertEquals(1, inputContext.get(AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.CURRENT_PORTFOLIO.name(), List.class).size());
        Mockito.verify(agrPortfolioBusinessService, Mockito.times(1))
                .updateConnectionGroupPowerContainers(Matchers.anyListOf(ConnectionGroupPortfolioDto.class), Matchers.eq(today));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testHandleRetrieveAdsGoalRealizationEventWithEmptyPortfolio() {
        // variables and additional mocking
        Mockito.when(corePlanboardBusinessService
                .findActiveConnectionGroupStates(Matchers.any(LocalDate.class), Matchers.any(Class.class)))
                .thenReturn(new ArrayList<>());
        // invocation
        coordinator.handleRetrieveAdsGoalRealizationEvent(new AgrNonUdiRetrieveAdsGoalRealizationEvent());
        // verifications and assertions
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .findActiveConnectionGroupStates(Matchers.any(LocalDate.class), Matchers.isNull(Class.class));
        Mockito.verifyZeroInteractions(agrPortfolioBusinessService);
        Mockito.verifyZeroInteractions(workflowStepExecuter);
    }

    @Test(expected = WorkflowException.class)
    public void testHandleRetrieveAdsGoalRealizationEventWithMissingOutputFromPbc() {
        // variables and additional mocking
        Mockito.when(corePlanboardBusinessService
                .findActiveConnectionGroupStates(Matchers.any(LocalDate.class), Matchers.any(Class.class)))
                .thenReturn(buildActiveConnectionGroups());
        Mockito.when(workflowStepExecuter.invoke(
                Matchers.eq(AgrWorkflowStep.AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION.name()),
                Matchers.any(WorkflowContext.class))).then(call -> call.getArguments()[1]);
        // actual invocation
        coordinator.handleRetrieveAdsGoalRealizationEvent(new AgrNonUdiRetrieveAdsGoalRealizationEvent());
    }

    private List<ConnectionGroupPortfolioDto> buildConnectionGroupPortfolio() {
        ConnectionGroupPortfolioDto connectionGroupPortfolioDto = new ConnectionGroupPortfolioDto("brp.usef-example.com",
                USEFRoleDto.BRP);
        return Collections.singletonList(connectionGroupPortfolioDto);
    }

    private List<ConnectionGroupState> buildActiveConnectionGroups() {
        ConnectionGroup brpConnectionGroup = new BrpConnectionGroup("brp.usef-example.com");
        ConnectionGroupState connectionGroupState = new ConnectionGroupState();
        connectionGroupState.setConnectionGroup(brpConnectionGroup);
        return Collections.singletonList(connectionGroupState);
    }

    private WorkflowContext buildOutputContext(WorkflowContext inputContext) {
        List<ConnectionGroupPortfolioDto> connectionGroupPortfolioDtos = inputContext.get(
                AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.CURRENT_PORTFOLIO.name(), List.class);
        inputContext
                .setValue(AgrNonUdiRetrieveAdsGoalRealizationParameter.OUT.UPDATED_PORTFOLIO.name(), connectionGroupPortfolioDtos);
        return inputContext;
    }

}
