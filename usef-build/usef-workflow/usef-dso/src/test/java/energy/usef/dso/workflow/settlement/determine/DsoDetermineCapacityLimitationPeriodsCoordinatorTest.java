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

package energy.usef.dso.workflow.settlement.determine;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.model.ConnectionCapacityLimitationPeriod;
import energy.usef.dso.model.ConnectionMeterEvent;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.dto.ConnectionCapacityLimitationPeriodDto;

@RunWith(PowerMockRunner.class)
public class DsoDetermineCapacityLimitationPeriodsCoordinatorTest {

    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    public DsoDetermineCapacityLimitationPeriodsCoordinator dsoDetermineOrangeEventPeriodsCoordinator;

    @Before
    public void init() {
        dsoDetermineOrangeEventPeriodsCoordinator = new DsoDetermineCapacityLimitationPeriodsCoordinator();
        Whitebox.setInternalState(dsoDetermineOrangeEventPeriodsCoordinator, dsoPlanboardBusinessService);
        Whitebox.setInternalState(dsoDetermineOrangeEventPeriodsCoordinator, workflowStepExecuter);
    }

    @Test
    public void testHandleEvent() {
        LocalDate startDate = new LocalDate("2015-01-01");
        LocalDate endDate = new LocalDate("2015-01-31");

        List<ConnectionMeterEvent> connectionMeterEvents = new ArrayList<>();
        Mockito.when(dsoPlanboardBusinessService.findConnectionMeterEventsForPeriod(startDate, endDate))
                .thenReturn(connectionMeterEvents);

        WorkflowContext resultContext = new DefaultWorkflowContext();
        ArrayList<ConnectionCapacityLimitationPeriodDto> periods = new ArrayList<>();
        periods.add(new ConnectionCapacityLimitationPeriodDto());
        resultContext.setValue(DetermineReductionPeriodsStepParameter.OUT.CONNECTION_METER_EVENT_PERIODS.name(), periods);
        resultContext.setValue(DetermineOutagePeriodsStepParameter.OUT.CONNECTION_METER_EVENT_PERIODS.name(), periods);
        Mockito.when(workflowStepExecuter.invoke(Mockito.anyString(), Mockito.any(WorkflowContext.class))).thenReturn(resultContext);

        dsoDetermineOrangeEventPeriodsCoordinator.handleEvent(new DetermineOrangeEvent(startDate, endDate));

        Mockito.verify(workflowStepExecuter, Mockito.times(1)).invoke(
                Mockito.eq(DsoWorkflowStep.DSO_DETERMINE_ORANGE_OUTAGE_PERIODS.name()), Mockito.any(WorkflowContext.class));
        Mockito.verify(workflowStepExecuter, Mockito.times(1))
                .invoke(Mockito.eq(DsoWorkflowStep.DSO_DETERMINE_ORANGE_REDUCTION_PERIODS.name()), Mockito.any(WorkflowContext.class));
        Mockito.verify(workflowStepExecuter, Mockito.times(1))
                .invoke(Mockito.eq(DsoWorkflowStep.DSO_DETERMINE_ORANGE_REGIME_COMPENSATIONS.name()),
                        Mockito.any(WorkflowContext.class));

        Mockito.verify(dsoPlanboardBusinessService, Mockito.times(2))
                .storeConnectionMeterEventPeriod(Mockito.any(ConnectionCapacityLimitationPeriod.class));
    }
}
