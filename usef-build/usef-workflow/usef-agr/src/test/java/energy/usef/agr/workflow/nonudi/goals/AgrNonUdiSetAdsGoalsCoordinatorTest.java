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

package energy.usef.agr.workflow.nonudi.goals;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrNonUdiSetAdsGoalsCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrNonUdiSetAdsGoalsCoordinatorTest {

    private AgrNonUdiSetAdsGoalsCoordinator coordinator;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private ConfigAgr configAgr;

    @Mock
    private Config config;

    @Mock
    private EventValidationService eventValidationService;

    @Before
    public void setUp() throws Exception {
        coordinator = new AgrNonUdiSetAdsGoalsCoordinator();
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, configAgr);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, eventValidationService);

        Mockito.when(configAgr.getBooleanProperty(Matchers.eq(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR))).thenReturn(true);
        Mockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.PTU_DURATION))).thenReturn(15);

        Mockito.when(corePlanboardBusinessService.findActiveConnectionGroupStates(Matchers.any(LocalDate.class), Matchers.any()))
                .thenReturn(buildConnectionGroupStates());
        Mockito.when(
                corePlanboardBusinessService.findLastPrognoses(Matchers.eq(new LocalDate()), Matchers.eq("brp1.usef-example.com")))
                .thenReturn(buildPrognoses("brp1.usef-example.com"));
        Mockito.when(corePlanboardBusinessService
                .findLastPrognoses(Matchers.eq(new LocalDate()), Matchers.eq("ean.1234-1234-123123123123")))
                .thenReturn(buildPrognoses("ean.1234-1234-123123123123"));

        Mockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(Mockito.anyLong(), Mockito.any(), Mockito.any()))
                .thenReturn(new PlanboardMessage());
    }

    @Test
    public void testSetGoalsWithSpecificCongestionPoint() throws Exception {
        coordinator.handleEvent(new AgrNonUdiSetAdsGoalsEvent(new LocalDate(), "ean.1234-1234-123123123123"));

        Mockito.verify(workflowStepExecuter, Mockito.times(1)).invoke(Matchers.any(), Matchers.any());
    }

    @Test
    public void testSetGoalsWithSpecificBrp() throws Exception {
        coordinator.handleEvent(new AgrNonUdiSetAdsGoalsEvent(new LocalDate(), "brp1.usef-example.com"));

        Mockito.verify(workflowStepExecuter, Mockito.times(1)).invoke(Matchers.any(), Matchers.any());
    }

    @Test
    public void testSetGoalsWithUnknownUsefIdentifier() throws Exception {
        coordinator.handleEvent(new AgrNonUdiSetAdsGoalsEvent(new LocalDate(), "brp2.usef-example.com"));

        Mockito.verify(workflowStepExecuter, Mockito.times(0)).invoke(Matchers.any(), Matchers.any());
    }

    private List<ConnectionGroupState> buildConnectionGroupStates() {
        List<ConnectionGroupState> connectionGroupStateList = new ArrayList<>();

        connectionGroupStateList.add(buildConnectionGroupState("brp1.usef-example.com"));
        connectionGroupStateList.add(buildConnectionGroupState("ean.1234-1234-123123123123"));

        return connectionGroupStateList;
    }

    private ConnectionGroupState buildConnectionGroupState(String usefIdentifier) {
        ConnectionGroupState connectionGroupState = new ConnectionGroupState();
        connectionGroupState.setConnectionGroup(buildConnectionGroup(usefIdentifier));

        return connectionGroupState;
    }

    private ConnectionGroup buildConnectionGroup(String usefIdentifier) {
        ConnectionGroup connectionGroup;
        if (usefIdentifier.substring(0, 3).equals("brp")) {
            connectionGroup = new BrpConnectionGroup();
        } else {
            connectionGroup = new CongestionPointConnectionGroup();
        }
        connectionGroup.setUsefIdentifier(usefIdentifier);
        return connectionGroup;
    }

    private List<PtuPrognosis> buildPrognoses(String usefIdentifier) {
        List<PtuPrognosis> ptuPrognoses = new ArrayList<>();

        IntStream.rangeClosed(1, 96).forEach(i -> {
            PtuPrognosis ptuPrognosis = new PtuPrognosis();
            ptuPrognosis.setConnectionGroup(buildConnectionGroup(usefIdentifier));
            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setPtuDate(new LocalDate());
            ptuContainer.setPtuIndex(i);
            ptuPrognosis.setPtuContainer(ptuContainer);
            ptuPrognosis.setSequence(1l);
            if (ptuPrognosis.getConnectionGroup() instanceof BrpConnectionGroup) {
                ptuPrognosis.setType(PrognosisType.A_PLAN);
            } else {
                ptuPrognosis.setType(PrognosisType.D_PROGNOSIS);
            }
            ptuPrognosis.setPower(BigInteger.valueOf(i * 10));
            ptuPrognoses.add(ptuPrognosis);
        });
        return ptuPrognoses;
    }

}
