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

package energy.usef.agr.workflow.operate.identifychangeforecast;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_IDENTIFY_CHANGE_IN_FORECAST;

import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.operate.deviation.DetectDeviationEvent;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.dto.PtuContainerDto;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.util.ArrayList;
import java.util.Arrays;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrIdentifyChangeInForecastCoordinator}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PtuUtil.class })
public class AgrIdentifyChangeInForecastCoordinatorTest {
    private static final int AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL = 2;
    private static final LocalDate WORKING_PERIOD = DateTimeUtil.getCurrentDate();
    private AgrIdentifyChangeInForecastCoordinator coordinator;
    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private Event<DetectDeviationEvent> detectDeviationEventManager;
    @Mock
    private Event<ReOptimizePortfolioEvent> reOptimizePortfolioEventManager;
    @Mock
    private Config config;
    @Mock
    private CorePlanboardValidatorService corePlanboardValidatorService;

    @Before
    public void init() throws Exception {
        coordinator = new AgrIdentifyChangeInForecastCoordinator();

        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, corePlanboardValidatorService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, "detectDeviationEventManager", detectDeviationEventManager);
        Whitebox.setInternalState(coordinator, "reOptimizePortfolioEventManager", reOptimizePortfolioEventManager);

        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        PowerMockito.when(corePlanboardBusinessService.findInitializedDaysOfPlanboard())
                .thenReturn(Arrays.asList(WORKING_PERIOD, WORKING_PERIOD.plusDays(1)));
    }

    private WorkflowContext parseContext(WorkflowContext context) {
        context.setValue(IdentifyChangeInForecastStepParameter.OUT.FORECAST_CHANGED.name(), true);
        context.setValue(IdentifyChangeInForecastStepParameter.OUT.FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST.name(),
                Arrays.asList(new PtuContainerDto(WORKING_PERIOD, 5), new PtuContainerDto(WORKING_PERIOD.plusDays(1), 2)));
        return context;
    }

    @Test
    public void testHandleEventWithNoPlanboard() {
        PowerMockito.when(corePlanboardBusinessService.findInitializedDaysOfPlanboard()).thenReturn(new ArrayList<>());
        coordinator.identifyChangesInForecast(new IdentifyChangeInForecastEvent());
        // verfify we fetched the planboard initialization dates.
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).findInitializedDaysOfPlanboard();
        // verify workflow has been interrupted.
        Mockito.verify(agrPortfolioBusinessService, Mockito.times(0)).findConnectionPortfolioDto(Matchers.any(LocalDate.class));
        Mockito.verify(detectDeviationEventManager, Mockito.times(0)).fire(Matchers.any(DetectDeviationEvent.class));
        Mockito.verify(reOptimizePortfolioEventManager, Mockito.times(0)).fire(Matchers.any(ReOptimizePortfolioEvent.class));
    }

    /**
     * Tests AgrIdentifyChangeInForecastCoordinator.invoke method.
     */
    @Test
    public void testHandleEventTriggerReoptimize() {
        Mockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(Matchers.any(LocalDate.class)))
                .thenReturn(new ArrayList<>());
        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_IDENTIFY_CHANGE_IN_FORECAST.name()), Mockito.any()))
                .then(call -> parseContext((WorkflowContext) call.getArguments()[1]));

        PowerMockito.mockStatic(PtuUtil.class);
        PowerMockito.when(PtuUtil.getPtuIndex(Mockito.any(LocalDateTime.class), Mockito.eq(15))).thenReturn(1);

        coordinator.identifyChangesInForecast(new IdentifyChangeInForecastEvent());
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).findInitializedDaysOfPlanboard();
        Mockito.verify(agrPortfolioBusinessService, Mockito.times(AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL))
                .findConnectionPortfolioDto(Matchers.any(LocalDate.class));
        Mockito.verify(reOptimizePortfolioEventManager, Mockito.times(2)).fire(Matchers.any(ReOptimizePortfolioEvent.class));

    }

    @Test
    public void testHandleEventTriggerDetectDeviations() {
        Mockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(Matchers.any(LocalDate.class)))
                .thenReturn(new ArrayList<>());
        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_IDENTIFY_CHANGE_IN_FORECAST.name()), Mockito.any()))
                .then(call -> parseContext((WorkflowContext) call.getArguments()[1]));
        PowerMockito
                .when(corePlanboardValidatorService.isPtuContainerWithinIntradayGateClosureTime(Mockito.any(PtuContainer.class)))
                .thenReturn(true);

        PowerMockito.mockStatic(PtuUtil.class);
        PowerMockito.when(PtuUtil.getPtuIndex(Mockito.any(LocalDateTime.class), Mockito.eq(15))).thenReturn(1);

        coordinator.identifyChangesInForecast(new IdentifyChangeInForecastEvent());
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).findInitializedDaysOfPlanboard();
        Mockito.verify(agrPortfolioBusinessService, Mockito.times(AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL))
                .findConnectionPortfolioDto(Matchers.any(LocalDate.class));
        Mockito.verify(detectDeviationEventManager, Mockito.times(1)).fire(Matchers.any(DetectDeviationEvent.class));

    }
}
