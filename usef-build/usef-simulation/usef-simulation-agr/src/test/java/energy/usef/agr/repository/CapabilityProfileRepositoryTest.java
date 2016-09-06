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

package energy.usef.agr.repository;

import static org.junit.Assert.assertTrue;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.dto.device.capability.IncreaseCapabilityDto;
import energy.usef.agr.dto.device.capability.InterruptCapabilityDto;
import energy.usef.agr.dto.device.capability.InterruptCapabilityTypeDto;
import energy.usef.agr.dto.device.capability.ProfileDto;
import energy.usef.agr.dto.device.capability.ReduceCapabilityDto;
import energy.usef.agr.dto.device.capability.ReportCapabilityDto;
import energy.usef.agr.dto.device.capability.ShiftCapabilityDto;
import energy.usef.core.config.AbstractConfig;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigAgr.class, AbstractConfig.class })
public class CapabilityProfileRepositoryTest {

    public static final String DEMO_FRIDGE = "DemoFridge";
    CapabilityProfileRepository capabilityProfileRepository = new CapabilityProfileRepository();

    @Mock
    private ConfigAgr configAgr;

    @Before
    public void init() {
        PowerMockito.mockStatic(ConfigAgr.class);
        PowerMockito.mockStatic(AbstractConfig.class);
        PowerMockito.when(ConfigAgr.getConfigurationFolder()).thenReturn("src/test/resources/energy/usef/agr/repository/");
        Whitebox.setInternalState(capabilityProfileRepository, configAgr);
    }

    @Test
    public void testReadFromConfigFileWithoutFile() {
        Mockito.when(ConfigAgr.getConfigurationFolder()).thenReturn("src/test/resources/energy/usef/agr/repository/not-existing/");
        Map<String, ProfileDto> stringProfileDtoMap = capabilityProfileRepository.readFromConfigFile();
        Assert.assertNotNull(stringProfileDtoMap);
        Assert.assertEquals(0, stringProfileDtoMap.size());
    }

    @Test
    public void testReadFromConfigFileWith() {
        Mockito.when(ConfigAgr.getConfigurationFolder()).thenReturn("src/test/resources/energy/usef/agr/repository/");
        Map<String, ProfileDto> stringProfileDtoMap = capabilityProfileRepository.readFromConfigFile();
        Assert.assertNotNull(stringProfileDtoMap);
        Assert.assertEquals(2,stringProfileDtoMap.size());
    }

    @Test
    public void testWriteProfiles() throws Exception {
        Map<String, ProfileDto> profiles = new HashMap<>();
        ProfileDto profileDto = new ProfileDto();

        profileDto.getCapabilities().add(new IncreaseCapabilityDto());
        profileDto.getCapabilities().add(new ReduceCapabilityDto());
        InterruptCapabilityDto interruptCapabilityDto = new InterruptCapabilityDto();
        interruptCapabilityDto.setType(InterruptCapabilityTypeDto.FULL);
        profileDto.getCapabilities().add(interruptCapabilityDto);
        profileDto.getCapabilities().add(new ShiftCapabilityDto());
        profileDto.getCapabilities().add(new ReportCapabilityDto());
        profiles.put(DEMO_FRIDGE, profileDto);


        String data = capabilityProfileRepository.writeProfiles(profiles);
        assertTrue(data.contains("IncreaseCapability"));
        assertTrue(data.contains("ReduceCapability"));
        assertTrue(data.contains("InterruptCapability"));
        assertTrue(data.contains("ShiftCapability"));
        assertTrue(data.contains("ReportCapability"));
        assertTrue(data.contains("FULL"));

        System.out.println(data);

        profiles = capabilityProfileRepository.readProfiles(data);

        assertTrue(profiles.get(DEMO_FRIDGE).getCapabilities().get(0) instanceof IncreaseCapabilityDto);
    }

}
