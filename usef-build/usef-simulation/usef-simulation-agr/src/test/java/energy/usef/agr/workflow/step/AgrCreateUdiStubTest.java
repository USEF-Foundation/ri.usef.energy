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

package energy.usef.agr.workflow.step;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.dto.ElementTypeDto;
import energy.usef.agr.dto.device.capability.IncreaseCapabilityDto;
import energy.usef.agr.dto.device.capability.ProfileDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.repository.CapabilityProfileRepository;
import energy.usef.agr.workflow.plan.connection.profile.CreateUdiStepParameter;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class AgrCreateUdiStubTest {

    @Mock
    private CapabilityProfileRepository capabilityProfileRepository;

    private AgrCreateUdiStub stub = new AgrCreateUdiStub();

    @Before
    public void init() {
        Whitebox.setInternalState(stub, capabilityProfileRepository);
    }

    @Test
    public void testInvoke() throws Exception {
        Mockito.when(capabilityProfileRepository.readFromConfigFile()).thenReturn(buildProfiles());

        WorkflowContext context = stub.invoke(buildInputContext());

        Map<String, List<UdiEventDto>> results = (Map<String, List<UdiEventDto>>) context
                .getValue(CreateUdiStepParameter.OUT.UDI_EVENTS_PER_UDI_MAP.name());

        assertNotNull(results);
        List<UdiEventDto> events = results.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        assertEquals(3, events.size());
        assertEquals(1, events.get(0).getDeviceCapabilities().size());

    }

    private Map<String, ProfileDto> buildProfiles() {
        Map<String, ProfileDto> result = new HashMap<>();
        ProfileDto profileDto = new ProfileDto();
        profileDto.getCapabilities().add(new IncreaseCapabilityDto());
        result.put("DemoFridge", profileDto);
        return result;
    }

    private WorkflowContext buildInputContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(CreateUdiStepParameter.IN.PERIOD.name(), new LocalDate());
        context.setValue(CreateUdiStepParameter.IN.PTU_DURATION.name(), 15);
        context.setValue(CreateUdiStepParameter.IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), buildConnectionPortfolioDtos());
        context.setValue(CreateUdiStepParameter.IN.ELEMENT_PER_CONNECTION_MAP.name(), buildElementMap());
        return context;
    }

    private Map<String, List<ElementDto>> buildElementMap() {
        Map<String, List<ElementDto>> resultMap = new HashMap<>();
        IntStream.rangeClosed(1, 3).mapToObj(index -> "ean.10000000000" + index)
                .forEach(entityAddress -> {
                    resultMap.put(entityAddress, buildElements(entityAddress));
                });
        return resultMap;
    }

    private List<ElementDto> buildElements(String entityAddress) {
        List<ElementDto> list = new ArrayList<>();
        ElementDto dto = new ElementDto();
        dto.setConnectionEntityAddress(entityAddress);
        dto.setDtuDuration(5);
        dto.setProfile("DemoFridge");
        dto.setElementType(ElementTypeDto.MANAGED_DEVICE);
        dto.setId("DemoFridge1");
        list.add(dto);
        return list;
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolioDtos() {
        List<ConnectionPortfolioDto> portfolio = new ArrayList<>();
        IntStream.rangeClosed(1, 3).mapToObj(index -> new ConnectionPortfolioDto("ean.10000000000" + index))
                .forEach(connectionPortfolioDTO -> {
                    portfolio.add(connectionPortfolioDTO);
                });
        return portfolio;
    }
}
