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

package energy.usef.agr.workflow.operate.netdemand;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_DETERMINE_NET_DEMANDS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.service.business.AgrDeviceCapabilityBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.operate.netdemand.DetermineNetDemandStepParameter.OUT;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.exception.WorkflowException;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class AgrDetermineNetDemandCoordinatorTest {

    private static final LocalDate PERIOD = new LocalDate(2015, 11, 27);

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Mock
    private AgrDeviceCapabilityBusinessService agrDeviceCapabilityBusinessService;
    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private Config config;

    private AgrDetermineNetDemandCoordinator coordinator;

    @Before
    public void init() throws Exception {
        coordinator = new AgrDetermineNetDemandCoordinator();
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, agrDeviceCapabilityBusinessService);
        when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);

        when(corePlanboardBusinessService.findInitializedDaysOfPlanboard()).thenReturn(Arrays.asList(PERIOD, PERIOD.plusDays(1)));
    }

    @Test
    public void invokeDetermineNetDemandsWithoutConnections() {

        when(agrPortfolioBusinessService.findConnectionPortfolioDto(any(LocalDate.class), any(LocalDate.class))).thenReturn(
                new HashMap<>());
        coordinator.handleEvent(new DetermineNetDemandEvent());

        verify(agrPortfolioBusinessService, times(1)).findConnectionPortfolioDto(eq(PERIOD), eq(PERIOD.plusDays(1)));
        verifyZeroInteractions(workflowStepExecuter);
    }

    @Test
    public void invokeDetermineNetDemandsWithConnectionsExpectingContextValidationException() {
        when(agrPortfolioBusinessService.findConnectionPortfolioDto(any(LocalDate.class), any(LocalDate.class))).then(
                call -> buildConnectionPortfolioDto((LocalDate) call.getArguments()[0], (LocalDate) call.getArguments()[1],
                        "ean.0000000001"));

        ArgumentCaptor<WorkflowContext> workflowIn = ArgumentCaptor.forClass(WorkflowContext.class);

        when(workflowStepExecuter.invoke(eq(AGR_DETERMINE_NET_DEMANDS.name()), any())).thenReturn(new DefaultWorkflowContext());
        try {
            coordinator.handleEvent(new DetermineNetDemandEvent());
            Assert.fail("Expecting Context Validation error ");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof WorkflowException);
        }

        verify(agrPortfolioBusinessService, times(1)).findConnectionPortfolioDto(eq(PERIOD), eq(PERIOD.plusDays(1)));
        verify(workflowStepExecuter, times(1)).invoke(eq(AGR_DETERMINE_NET_DEMANDS.name()), workflowIn.capture());
    }

    @Test
    public void invokeDetermineNetDemandsHappyFlow() {
        Map<LocalDate, List<ConnectionPortfolioDto>> connectionPortfolioPerDay = buildConnectionPortfolioDto(PERIOD,
                PERIOD.plusDays(1),
                "ean.0000000001");
        when(agrPortfolioBusinessService.findConnectionPortfolioDto(any(LocalDate.class), any(LocalDate.class))).thenReturn(
                connectionPortfolioPerDay);

        WorkflowContext outContext = new DefaultWorkflowContext();

        outContext.setValue(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), connectionPortfolioPerDay.get(PERIOD));
        outContext.setValue(OUT.UPDATED_UDI_EVENT_DTO_LIST.name(), Collections.singletonList(new UdiEventDto()));
        when(workflowStepExecuter.invoke(eq(AGR_DETERMINE_NET_DEMANDS.name()), any())).thenReturn(outContext);

        coordinator.handleEvent(new DetermineNetDemandEvent());

        verify(agrPortfolioBusinessService, times(1)).findConnectionPortfolioDto(eq(PERIOD), eq(PERIOD.plusDays(1)));
        verify(workflowStepExecuter, times(2)).invoke(eq(AGR_DETERMINE_NET_DEMANDS.name()), any());
        verify(agrPortfolioBusinessService, times(2)).updateConnectionPortfolio(any(LocalDate.class),
                Mockito.anyListOf(ConnectionPortfolioDto.class));
        verify(agrDeviceCapabilityBusinessService, times(2)).updateUdiEvents(any(LocalDate.class),
                Mockito.anyListOf(UdiEventDto.class));

    }

    private Map<LocalDate, List<ConnectionPortfolioDto>> buildConnectionPortfolioDto(LocalDate startDate, LocalDate endDate,
            String connectionEntityAddress) {
        return DateTimeUtil.generateDatesOfInterval(startDate, endDate)
                .stream()
                .collect(Collectors.toMap(Function.identity(),
                        period -> Collections.singletonList(new ConnectionPortfolioDto(connectionEntityAddress))));
    }
}
