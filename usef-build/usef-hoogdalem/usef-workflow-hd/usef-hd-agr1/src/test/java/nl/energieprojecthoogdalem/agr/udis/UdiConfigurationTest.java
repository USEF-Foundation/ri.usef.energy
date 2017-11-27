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

package nl.energieprojecthoogdalem.agr.udis;

import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class UdiConfigurationTest
{
    @Test
    public void TestReadFiles()
    {
        UdiConfiguration udiConfiguration = new UdiConfiguration();

        Map<String, String> endpointsMap = udiConfiguration.getEndpoints();

        assertEquals("MZ29EBX0BJ", endpointsMap.get("22"));

        assertNotNull(udiConfiguration.getCapabilities(ElementType.BATTERY_ZIH));
        assertNotNull(udiConfiguration.getCapabilities(ElementType.BATTERY_NOD));
    }

}