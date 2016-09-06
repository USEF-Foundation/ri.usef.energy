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

package energy.usef.agr.service.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.agr.dto.device.capability.IncreaseCapabilityDto;
import energy.usef.agr.dto.device.capability.ReportCapabilityDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.dto.device.capability.UdiEventTypeDto;
import energy.usef.agr.model.UdiEvent;
import energy.usef.agr.model.UdiEventType;
import energy.usef.agr.model.device.capability.IncreaseCapability;
import energy.usef.agr.model.device.capability.ReportCapability;
import energy.usef.agr.model.device.capability.ShiftCapability;
import energy.usef.agr.repository.UdiRepository;
import energy.usef.agr.repository.UdiEventRepository;
import energy.usef.agr.repository.device.capability.IncreaseCapabilityRepository;
import energy.usef.agr.repository.device.capability.InterruptCapabilityRepository;
import energy.usef.agr.repository.device.capability.ReduceCapabilityRepository;
import energy.usef.agr.repository.device.capability.ReportCapabilityRepository;
import energy.usef.agr.repository.device.capability.ShiftCapabilityRepository;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class AgrDeviceCapabilityBusinessServiceTest {

    private AgrDeviceCapabilityBusinessService agrDeviceCapabilityBusinessService;
    @Mock
    private UdiRepository udiRepository;
    @Mock
    private UdiEventRepository udiEventRepository;
    @Mock
    private ShiftCapabilityRepository shiftCapabilityRepository;
    @Mock
    private InterruptCapabilityRepository interruptCapabilityRepository;
    @Mock
    private IncreaseCapabilityRepository increaseCapabilityRepository;
    @Mock
    private ReduceCapabilityRepository reduceCapabilityRepository;
    @Mock
    private ReportCapabilityRepository reportCapabilityRepository;

    @Before
    public void setUp() {
        agrDeviceCapabilityBusinessService = new AgrDeviceCapabilityBusinessService();
        setInternalState(agrDeviceCapabilityBusinessService, udiRepository);
        setInternalState(agrDeviceCapabilityBusinessService, udiEventRepository);
        setInternalState(agrDeviceCapabilityBusinessService, shiftCapabilityRepository);
        setInternalState(agrDeviceCapabilityBusinessService, interruptCapabilityRepository);
        setInternalState(agrDeviceCapabilityBusinessService, increaseCapabilityRepository);
        setInternalState(agrDeviceCapabilityBusinessService, reduceCapabilityRepository);
        setInternalState(agrDeviceCapabilityBusinessService, reportCapabilityRepository);
    }

    @Test
    public void testUpdateUdiEvents() throws Exception {
        final LocalDate period = new LocalDate(2015, 11, 9);
        PowerMockito.when(udiEventRepository.findUdiEventsForPeriod(eq(period))).thenReturn(buildDatabaseUdiEvents(period));
        // invocation
        agrDeviceCapabilityBusinessService.updateUdiEvents(period, buildUdiEventDtos(period));
        // assertions and verifications
        ArgumentCaptor<UdiEvent> toBeDeletedCaptor = ArgumentCaptor.forClass(UdiEvent.class);
        ArgumentCaptor<UdiEvent> toBeCreatedCaptor = ArgumentCaptor.forClass(UdiEvent.class);
        verify(udiEventRepository, times(1)).findUdiEventsForPeriod(eq(period));
        verify(udiEventRepository, times(1)).delete(toBeDeletedCaptor.capture());
        verify(udiEventRepository, times(1)).persist(toBeCreatedCaptor.capture());
        verify(reportCapabilityRepository, times(2)).persist(any(ReportCapability.class));
        verify(increaseCapabilityRepository, times(1)).persist(any(IncreaseCapability.class));
        assertEquals("2222-tobedeleted-2222", toBeDeletedCaptor.getValue().getId());
        assertEquals("3333-tobecreated-3333", toBeCreatedCaptor.getValue().getId());
    }

    private List<UdiEvent> buildDatabaseUdiEvents(LocalDate period) {
        UdiEvent event1 = new UdiEvent();
        event1.setPeriod(period);
        event1.setId("1111-tobeupdated-1111");
        event1.setDeviceSelector("usef://device1.usef.energy");
        event1.setStartDtu(1);
        event1.setEndDtu(5);
        event1.setUdiEventType(UdiEventType.CONSUMPTION);
        buildDeviceCapabilites(event1);
        UdiEvent event2 = new UdiEvent();
        event2.setPeriod(period);
        event2.setId("2222-tobedeleted-2222");
        return Arrays.asList(event1, event2);
    }

    private void buildDeviceCapabilites(UdiEvent udiEvent) {
        ShiftCapability shiftCapability = new ShiftCapability();
        shiftCapability.setId("0000.shift.0.tobedeleted");
        shiftCapability.setUdiEvent(udiEvent);
        IncreaseCapability increaseCapability = new IncreaseCapability();
        increaseCapability.setId("0000.increase.1.tobeupdated");
        increaseCapability.setMaxPower(BigInteger.valueOf(2000));
        increaseCapability.setDurationMultiplier(2);
        increaseCapability.setPowerStep(BigInteger.TEN);
        increaseCapability.setMaxDtus(10);
        increaseCapability.setUdiEvent(udiEvent);
        udiEvent.getDeviceCapabilities().add(shiftCapability);
        udiEvent.getDeviceCapabilities().add(increaseCapability);

    }

    private List<UdiEventDto> buildUdiEventDtos(LocalDate period) {
        UdiEventDto event1 = new UdiEventDto();
        event1.setId("1111-tobeupdated-1111");
        event1.setDeviceSelector("usef://device1.usef.energy.UPDATED");
        event1.setStartDtu(6);
        event1.setEndDtu(10);
        event1.setUdiEventType(UdiEventTypeDto.ON_DEMAND_CONSUMPTION);
        event1.setPeriod(period);
        buildDeviceCapabilitesDto(event1);
        UdiEventDto event2 = new UdiEventDto();
        event2.setId("3333-tobecreated-3333");
        event2.setDeviceSelector("usef://device2.usef.energy.CREATED");
        event2.setStartDtu(11);
        event2.setEndDtu(15);
        event2.setUdiEventType(UdiEventTypeDto.PRODUCTION);
        event2.setPeriod(period);
        buildDeviceCapabilitesDto(event2);
        return Arrays.asList(event1, event2);
    }

    private void buildDeviceCapabilitesDto(UdiEventDto udiEventDto) {
        ReportCapabilityDto reportCapabilityDto = new ReportCapabilityDto();
        reportCapabilityDto.setId("0000.report.1.tobecreated");
        IncreaseCapabilityDto increaseCapabilityDto = new IncreaseCapabilityDto();
        increaseCapabilityDto.setId("0000.increase.1.tobeupdated");
        increaseCapabilityDto.setMaxPower(BigInteger.valueOf(1000));
        increaseCapabilityDto.setDurationMultiplier(1);
        increaseCapabilityDto.setPowerStep(BigInteger.TEN);
        increaseCapabilityDto.setMaxDtus(5);
        udiEventDto.getDeviceCapabilities().add(reportCapabilityDto);
        udiEventDto.getDeviceCapabilities().add(increaseCapabilityDto);
    }
}
