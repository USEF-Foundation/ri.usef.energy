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

package energy.usef.agr.workflow.plan.connection.profile;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.model.Element;
import energy.usef.agr.service.business.AgrElementBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.util.ArrayList;
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

/**
 * Test class in charge of the unit tests related to the {@link AgrUpdateElementDataStoreCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrUpdateElementDataStoreCoordinatorTest {

    private final static LocalDate PERIOD = new LocalDate("2015-10-10");

    private AgrUpdateElementDataStoreCoordinator coordinator;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private Config config;

    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Mock
    private AgrElementBusinessService agrElementBusinessService;

    @Mock
    private Event<CreateConnectionProfileEvent> createConnectionProfileEventManager;

    @Mock
    private EventValidationService eventValidationService;

    @Before
    public void init() {
        coordinator = new AgrUpdateElementDataStoreCoordinator();
        Whitebox.setInternalState(coordinator, "workflowStepExecuter", workflowStepExecuter);
        Whitebox.setInternalState(coordinator, "config", config);
        Whitebox.setInternalState(coordinator, "agrPortfolioBusinessService", agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, "createConnectionProfileEventManager", createConnectionProfileEventManager);
        Whitebox.setInternalState(coordinator, "agrElementBusinessService", agrElementBusinessService);
        Whitebox.setInternalState(coordinator, eventValidationService);
    }

    @Test
    public void testUpdateElementDataStore() throws Exception {
        Mockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(120);
        Mockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(PERIOD))
                .thenReturn(new ArrayList<ConnectionPortfolioDto>());
        Mockito.when(workflowStepExecuter.invoke(Matchers.anyString(), Matchers.any(WorkflowContext.class)))
                .then(invocationOnMock -> {
                    WorkflowContext context = (WorkflowContext) invocationOnMock.getArguments()[1];

                    List<ElementDto> elementDtoList = new ArrayList<>();
                    context.setValue(AgrUpdateElementDataStoreParameter.OUT.ELEMENT_LIST.name(), elementDtoList);
                    return context;
                });
        coordinator.updateElementDataStore(new AgrUpdateElementDataStoreEvent(PERIOD));

        Mockito.verify(workflowStepExecuter, Mockito.times(1)).invoke(Matchers.anyString(), Matchers.any(WorkflowContext.class));
        Mockito.verify(agrElementBusinessService, Mockito.times(1)).createElements(Matchers.anyListOf(Element.class));
        Mockito.verify(createConnectionProfileEventManager, Mockito.times(1))
                .fire(Matchers.any(CreateConnectionProfileEvent.class));
    }
}
