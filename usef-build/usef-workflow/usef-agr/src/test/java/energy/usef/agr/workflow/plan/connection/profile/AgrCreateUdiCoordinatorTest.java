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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.service.business.AgrDeviceCapabilityBusinessService;
import energy.usef.agr.service.business.AgrElementBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class AgrCreateUdiCoordinatorTest {

    private AgrCreateUdiCoordinator agrCreateUdiCoordinator;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private Config config;

    @Mock
    private ConfigAgr configAgr;

    @Mock
    private AgrElementBusinessService agrElementBusinessService;

    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Mock
    private AgrDeviceCapabilityBusinessService agrDeviceCapabilityBusinessService;

    @Mock
    private EventValidationService eventValidationService;

    @Before
    public void init() {
        agrCreateUdiCoordinator = new AgrCreateUdiCoordinator();
        Whitebox.setInternalState(agrCreateUdiCoordinator, workflowStepExecuter);
        Whitebox.setInternalState(agrCreateUdiCoordinator, config);
        Whitebox.setInternalState(agrCreateUdiCoordinator, configAgr);
        Whitebox.setInternalState(agrCreateUdiCoordinator, agrElementBusinessService);
        Whitebox.setInternalState(agrCreateUdiCoordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(agrCreateUdiCoordinator, agrDeviceCapabilityBusinessService);
        Whitebox.setInternalState(agrCreateUdiCoordinator, eventValidationService);

        when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL)).thenReturn(1);

        when(workflowStepExecuter.invoke(anyString(), any(WorkflowContext.class))).thenReturn(buildContext());



    }

    @Test
    public void testCreateUdis() throws Exception {
        LocalDate period = new LocalDate();

        when(agrPortfolioBusinessService.findConnectionPortfolioDto(period)).thenReturn(buildConnectionPortfolioDto());
        when(agrElementBusinessService.findElementDtos(period)).thenReturn(buildElements());

        agrCreateUdiCoordinator.createUdis(new CreateUdiEvent(period));

        verify(agrPortfolioBusinessService, times(1)).createUdis(eq(period), anyList());
        verify(agrDeviceCapabilityBusinessService, times(1)).updateUdiEvents(eq(period), anyList());
    }

    private List<ElementDto> buildElements() {
        List<ElementDto> results = new ArrayList<>();
        ElementDto elementDto = new ElementDto();
        elementDto.setId("1");
        elementDto.setDtuDuration(15);
        elementDto.setConnectionEntityAddress("ean.00001");
        results.add(elementDto);
        return results;
    }

    private WorkflowContext buildContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(CreateUdiStepParameter.OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), buildConnectionPortfolioDto());
        context.setValue(CreateUdiStepParameter.OUT.UDI_EVENTS_PER_UDI_MAP.name(), new HashMap<>());

        return context;
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolioDto() {
        List<ConnectionPortfolioDto> results = new ArrayList<>();
        ConnectionPortfolioDto connectionPortfolioDto = new ConnectionPortfolioDto("ean.00001");
        results.add(connectionPortfolioDto);
        return results;

    }
}
