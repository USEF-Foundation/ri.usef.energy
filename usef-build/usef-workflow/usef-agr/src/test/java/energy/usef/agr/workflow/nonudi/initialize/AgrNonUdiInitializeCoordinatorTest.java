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

package energy.usef.agr.workflow.nonudi.initialize;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrNonUdiInitializeCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class AgrNonUdiInitializeCoordinatorTest {

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private ConfigAgr configAgr;

    @Mock
    private EventValidationService eventValidationService;

    private AgrNonUdiInitializeCoordinator coordinator;

    @Before
    public void init() throws Exception {
        coordinator = new AgrNonUdiInitializeCoordinator();
        Whitebox.setInternalState(coordinator, configAgr);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, eventValidationService);
    }

    /**
     * Test if coordinator doesn't do anything for udi enabled aggregators.
     */
    @Test
    public void testInitializeClusterWithUdiAgr() throws BusinessValidationException {
        PowerMockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(false);

        coordinator.initializeCluster(new AgrNonUdiInitializeEvent(DateTimeUtil.getCurrentDate()));

        Mockito.verify(workflowStepExecuter, Mockito.times(0)).invoke(Matchers.any(), Matchers.any());
    }

    /**
     * Test if coordinator triggers the PBC for non-udi enabled aggregators.
     */
    @Test
    public void testInitializeClusterWithNonUdiAgr() throws BusinessValidationException {
        PowerMockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(true);

        coordinator.initializeCluster(new AgrNonUdiInitializeEvent(DateTimeUtil.getCurrentDate()));

        Mockito.verify(workflowStepExecuter, Mockito.times(1)).invoke(Matchers.any(), Matchers.any());
    }

}
