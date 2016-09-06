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

package energy.usef.brp.workflow.plan.aplan.missing;

import energy.usef.brp.service.business.BrpPlanboardBusinessService;
import energy.usef.brp.workflow.BrpWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.event.DayAheadClosureEvent;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.model.AgrConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
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
 * Unit test responsible for testing the {@link BrpCreateMissingAPlansCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class BrpCreateMissingAPlansCoordinatorTest {

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private BrpPlanboardBusinessService brpPlanboardBusinessService;

    @Mock
    private EventValidationService eventValidationService;

    @Mock
    private Config config;

    BrpCreateMissingAPlansCoordinator coordinator;

    @Before
    public void init() throws Exception {
        coordinator = new BrpCreateMissingAPlansCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();

        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, brpPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);
        Whitebox.setInternalState(coordinator, eventValidationService);

        Mockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        Mockito.when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("brp.usef-example.com");
        Mockito.when(corePlanboardBusinessService.findActiveConnectionGroupsWithConnections(Matchers.any(LocalDate.class)))
                .thenReturn(buildActiveConnectionGroupsWithConnections());
    }

    @Test
    public void testHandleEventWithMissingAPlans() throws Exception {
        Mockito.when(
                corePlanboardBusinessService.findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(PrognosisType.A_PLAN)))
                .thenReturn(buildAPlans(1));
        Mockito.when(workflowStepExecuter
                .invoke(Matchers.eq(BrpWorkflowStep.BRP_CREATE_MISSING_A_PLANS.name()), Matchers.any(WorkflowContext.class)))
                .thenReturn(buildPbcOutput());

        coordinator.handleEvent(new DayAheadClosureEvent());

        Mockito.verify(workflowStepExecuter, Mockito.times(1))
                .invoke(Matchers.eq(BrpWorkflowStep.BRP_CREATE_MISSING_A_PLANS.name()), Matchers.any(WorkflowContext.class));

        ArgumentCaptor<Prognosis> captor = ArgumentCaptor.forClass(Prognosis.class);

        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .storePrognosis(Matchers.any(Prognosis.class), Matchers.any(AgrConnectionGroup.class),
                        Matchers.eq(DocumentType.A_PLAN), Matchers.eq(DocumentStatus.ACCEPTED),
                        Matchers.eq("agr2.usef-example.com"), (Message) Matchers.isNull(), Matchers.eq(true));
    }

    @Test
    public void testHandleEventWithNoMissingAPlans() throws Exception {
        Mockito.when(
                corePlanboardBusinessService.findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(PrognosisType.A_PLAN)))
                .thenReturn(buildAPlans(2));

        coordinator.handleEvent(new DayAheadClosureEvent());

        Mockito.verify(workflowStepExecuter, Mockito.times(0))
                .invoke(Matchers.eq(BrpWorkflowStep.BRP_CREATE_MISSING_A_PLANS.name()), Matchers.any(WorkflowContext.class));
    }

    private WorkflowContext buildPbcOutput() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(BrpCreateMissingAPlansParamater.OUT.PROGNOSIS_DTO.name(), new PrognosisDto());
        return context;
    }

    private List<PtuPrognosis> buildAPlans(int agrCount) {
        List<PtuPrognosis> prognosis = new ArrayList<>();

        IntStream.rangeClosed(1, agrCount).forEach(agr -> IntStream.rangeClosed(1, 96).forEach(ptuIndex -> {
            PtuPrognosis ptuPrognosis = new PtuPrognosis();
            ptuPrognosis.setPtuContainer(new PtuContainer(DateTimeUtil.getCurrentDate().plusDays(1), ptuIndex));
            AgrConnectionGroup agrConnectionGroup = new AgrConnectionGroup("agr" + agr + ".usef-example.com");
            agrConnectionGroup.setAggregatorDomain("agr" + agr + ".usef-example.com");
            ptuPrognosis.setConnectionGroup(agrConnectionGroup);
            ptuPrognosis.setType(PrognosisType.A_PLAN);
            ptuPrognosis.setPower(BigInteger.valueOf(ptuIndex * 10));

            prognosis.add(ptuPrognosis);
        }));


        return prognosis;
    }

    private Map<ConnectionGroup, List<Connection>> buildActiveConnectionGroupsWithConnections() {
        Map<ConnectionGroup, List<Connection>> toReturn = new HashMap<>();

        IntStream.rangeClosed(1, 2).forEach(connectionGroupId -> {
            AgrConnectionGroup connectionGroup = new AgrConnectionGroup("agr" + connectionGroupId + ".usef-example.com");
            connectionGroup.setAggregatorDomain(connectionGroup.getUsefIdentifier());
            toReturn.put(connectionGroup, buildConnections());
        });

        return toReturn;
    }

    private List<Connection> buildConnections() {
        List<Connection> connectionList = new ArrayList<>();

        IntStream.rangeClosed(1, 10).forEach(connectionId -> {
            Connection connection = new Connection();
            connection.setEntityAddress("ean." + connectionId);
            connectionList.add(connection);
        });

        return connectionList;
    }
}
