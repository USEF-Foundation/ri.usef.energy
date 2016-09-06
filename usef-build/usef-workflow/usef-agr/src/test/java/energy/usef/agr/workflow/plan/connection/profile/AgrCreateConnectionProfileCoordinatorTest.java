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

import static org.mockito.Mockito.*;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.service.business.AgrElementBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.exception.WorkflowException;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.util.ArrayList;
import java.util.Collections;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.junit.Assert;
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
 * Test class in charge of the unit tests related to the {@link AgrCreateConnectionProfileCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrCreateConnectionProfileCoordinatorTest {

    private static final int PLANBOARD_INITIALIZATION_DURATION = 2;

    @Mock
    private Config config;
    @Mock
    private ConfigAgr configAgr;
    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Mock
    private AgrElementBusinessService agrElementBusinessService;
    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private Event<CreateUdiEvent> createUdiEventManager;
    @Mock
    private EventValidationService eventValidationService;

    private AgrCreateConnectionProfileCoordinator coordinator;

    @Before
    public void setUp() {
        coordinator = new AgrCreateConnectionProfileCoordinator();
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configAgr);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, agrElementBusinessService);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, createUdiEventManager);
        Whitebox.setInternalState(coordinator, eventValidationService);
        Mockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(false);

    }

    @Test
    public void testCreateConnectionProfileHappyFlow() throws BusinessValidationException {
        // mocking and variables
        final LocalDate initializationDate = new LocalDate(2015, 8, 21);
        mockAgrPortfolioBusinessService();
        mockWorkflowStepLoader();
        mockConfig();
        // invocation
        coordinator.createConnectionProfile(new CreateConnectionProfileEvent(initializationDate));
        // verifications
        verify(config, times(1)).getIntegerProperty(ConfigParam.PTU_DURATION);
        verify(agrPortfolioBusinessService, times(PLANBOARD_INITIALIZATION_DURATION))
                .findConnectionPortfolioDto(Matchers.any(LocalDate.class));
        verify(agrElementBusinessService, times(PLANBOARD_INITIALIZATION_DURATION)).findElementDtos(Matchers.any(LocalDate.class));
        verify(configAgr, times(1)).getIntegerProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL);
        verify(workflowStepExecuter, times(PLANBOARD_INITIALIZATION_DURATION))
                .invoke(Matchers.eq(AgrWorkflowStep.AGR_CREATE_CONNECTION_PROFILE.name()), Matchers.any(WorkflowContext.class));
        verify(agrPortfolioBusinessService, times(PLANBOARD_INITIALIZATION_DURATION))
                .createConnectionProfiles(Matchers.any(LocalDate.class), Matchers.anyListOf(ConnectionPortfolioDto.class));

        verify(createUdiEventManager, times(1)).fire(Mockito.any(CreateUdiEvent.class));
    }

    @Test
    public void testCreateConnectionProfileWithNoConnections() throws BusinessValidationException {
        // mocking and variables
        final LocalDate initializationDate = new LocalDate(2015, 8, 21);
        mockAgrPortfolioBusinessServiceWithNoConnections();
        mockWorkflowStepLoader();
        mockConfig();
        // invocation
        coordinator.createConnectionProfile(new CreateConnectionProfileEvent(initializationDate));
        // verifications
        verify(config, times(1)).getIntegerProperty(ConfigParam.PTU_DURATION);
        verify(agrPortfolioBusinessService, times(PLANBOARD_INITIALIZATION_DURATION))
                .findConnectionPortfolioDto(Matchers.any(LocalDate.class));
        verify(agrElementBusinessService, times(0)).findElementDtos(Matchers.any(LocalDate.class));
        verify(configAgr, times(1)).getIntegerProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL);
        verifyZeroInteractions(workflowStepExecuter);
        verify(agrPortfolioBusinessService, times(0))
                .createConnectionProfiles(Matchers.any(LocalDate.class), Matchers.anyListOf(ConnectionPortfolioDto.class));

        verify(createUdiEventManager, times(1)).fire(Mockito.any(CreateUdiEvent.class));
    }

    @Test
    public void testCreateConnectionProfileWithWrongPBCOutcome() throws BusinessValidationException {
        // mocking and variables
        final LocalDate initializationDate = new LocalDate(2015, 8, 21);
        mockAgrPortfolioBusinessService();
        mockWorkflowStepLoaderWithErrors();
        mockConfig();
        // invocation
        try {
            coordinator.createConnectionProfile(new CreateConnectionProfileEvent(initializationDate));
            Assert.fail("Expected WorkflowException since outputContext misses the list of connections.");
        } catch (WorkflowException e) {
            verify(config, times(1)).getIntegerProperty(ConfigParam.PTU_DURATION);
            verify(agrPortfolioBusinessService, times(1))
                    .findConnectionPortfolioDto(Matchers.any(LocalDate.class));
            verify(agrElementBusinessService, times(1)).findElementDtos(Matchers.any(LocalDate.class));
            verify(configAgr, times(1)).getIntegerProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL);
            verify(agrPortfolioBusinessService, times(0))
                    .createConnectionProfiles(Matchers.any(LocalDate.class), Matchers.anyListOf(ConnectionPortfolioDto.class));
            verify(createUdiEventManager, times(0)).fire(Mockito.any(CreateUdiEvent.class));
        }
    }

    private void mockWorkflowStepLoader() {
        PowerMockito.when(workflowStepExecuter.invoke(Matchers.eq(AgrWorkflowStep.AGR_CREATE_CONNECTION_PROFILE.name()),
                Matchers.any(WorkflowContext.class))).then(call -> call.getArguments()[1]);
    }

    private void mockWorkflowStepLoaderWithErrors() {
        PowerMockito.when(workflowStepExecuter.invoke(Matchers.eq(AgrWorkflowStep.AGR_CREATE_CONNECTION_PROFILE.name()),
                Matchers.any(WorkflowContext.class))).then(call -> new DefaultWorkflowContext());
    }

    private void mockAgrPortfolioBusinessService() {
        PowerMockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(Matchers.any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new ConnectionPortfolioDto("ean.0000000001")));
    }

    private void mockAgrPortfolioBusinessServiceWithNoConnections() {
        PowerMockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(Matchers.any(LocalDate.class)))
                .thenReturn(new ArrayList<>());
    }

    private void mockConfig() {
        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        PowerMockito.when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL)).thenReturn(
                PLANBOARD_INITIALIZATION_DURATION);
    }
}
