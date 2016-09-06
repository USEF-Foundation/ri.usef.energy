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

package energy.usef.dso.workflow.validate.gridsafetyanalysis;

import energy.usef.core.config.Config;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.event.DayAheadClosureEvent;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.model.AggregatorOnConnectionGroupState;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

@RunWith(PowerMockRunner.class)
public class DsoCreateMissingDPrognosisCoordinatorTest {

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private WorkflowStep workflowStep;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Mock
    private Config config;

    @Mock
    private EventValidationService eventValidationService;

    @Mock
    private Event<GridSafetyAnalysisEvent> gridSafetyEventManager;

    private DsoCreateMissingDPrognosisCoordinator dsoCreateMissingDPrognosisCoordinator;

    @Before
    public void setUp() throws Exception {
        dsoCreateMissingDPrognosisCoordinator = new DsoCreateMissingDPrognosisCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        Whitebox.setInternalState(dsoCreateMissingDPrognosisCoordinator, workflowStepExecuter);
        Whitebox.setInternalState(dsoCreateMissingDPrognosisCoordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(dsoCreateMissingDPrognosisCoordinator, dsoPlanboardBusinessService);
        Whitebox.setInternalState(dsoCreateMissingDPrognosisCoordinator, config);
        Whitebox.setInternalState(dsoCreateMissingDPrognosisCoordinator, gridSafetyEventManager);
        Whitebox.setInternalState(dsoCreateMissingDPrognosisCoordinator, sequenceGeneratorService);
        Whitebox.setInternalState(dsoCreateMissingDPrognosisCoordinator, eventValidationService);
    }

    @Test
    public void testHandleEvent() throws Exception {
        DayAheadClosureEvent event = new DayAheadClosureEvent();

        String entityAddress = "ean.1234";
        List<AggregatorOnConnectionGroupState> aggregatorList = new ArrayList<>();
        AggregatorOnConnectionGroupState aggregatorOnConnectionGroupState = new AggregatorOnConnectionGroupState();
        aggregatorList.add(aggregatorOnConnectionGroupState);
        aggregatorOnConnectionGroupState.setAggregator(new Aggregator());
        aggregatorOnConnectionGroupState.getAggregator().setDomain("agr.usef-example.com");
        aggregatorOnConnectionGroupState.setConnectionCount(BigInteger.TEN);
        aggregatorOnConnectionGroupState.setCongestionPointConnectionGroup(new CongestionPointConnectionGroup());
        aggregatorOnConnectionGroupState.getCongestionPointConnectionGroup().setUsefIdentifier(entityAddress);

        Mockito.when(dsoPlanboardBusinessService.findConnectionGroupsWithAggregators(Matchers.any(LocalDate.class))).thenReturn(
                Collections.singletonMap(new CongestionPointConnectionGroup("ean.1234"),
                        Collections.singletonList(aggregatorOnConnectionGroupState)));

        WorkflowContext resultWorkflowContext = new DefaultWorkflowContext();
        resultWorkflowContext.setValue(CreateMissingDPrognosisParameter.OUT.D_PROGNOSIS.name(), new PrognosisDto());

        Mockito.when(corePlanboardBusinessService.findActiveCongestionPointAddresses(event.getPeriod())).thenReturn(
                Collections.singletonList(entityAddress));
        Mockito.when(dsoPlanboardBusinessService.findAggregatorOnConnectionGroupStateByCongestionPointAddress(
                Mockito.eq(entityAddress), Mockito.any(LocalDate.class)))
                .thenReturn(aggregatorList);
        Mockito.when(workflowStepExecuter.invoke(Mockito.anyString(), Mockito.any())).thenReturn(resultWorkflowContext);

        dsoCreateMissingDPrognosisCoordinator.handleEvent(event);

        Mockito.verify(gridSafetyEventManager, Mockito.times(1)).fire(Mockito.any(GridSafetyAnalysisEvent.class));

        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .storePrognosis(Matchers.eq("ean.1234"), Matchers.any(Prognosis.class), Matchers.eq(DocumentType.D_PROGNOSIS),
                        Matchers.eq(DocumentStatus.ACCEPTED), Matchers.eq("agr.usef-example.com"), (Message) Matchers.isNull(),
                        Matchers.eq(true));
    }
}
