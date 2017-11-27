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
package nl.energieprojecthoogdalem.forecastservice.element;

import info.usef.agr.dto.ElementDto;
import info.usef.agr.dto.ElementDtuDataDto;
import info.usef.agr.dto.ElementTypeDto;


import java.math.BigInteger;

/**
 * default elements for the ZIH, NOD connections
 * */
public class DefaultElement
{
    /**
     * Creates an default home element for the given ean
     * @param ean an EAN string from the connections
     * @return ElementDto with default values
     * */
    public static ElementDto getHomeElement(String ean)
    {
        return  getElement(ean, ElementType.HOME, ElementType.HOME_ID, ElementTypeDto.SYNTHETIC_DATA);
    }

    /**
     * Creates an default battery element for the given ean
     * @param ean an EAN string from the connections
     * @return ElementDto with default values
     * */
    public static ElementDto getBatteryElement(String ean)
    {
        return  getElement(ean, ElementType.BATTERY, ElementType.BATTERY_ID, ElementTypeDto.MANAGED_DEVICE);
    }


    /**
     * Creates an default element for the given ean profile, type
     *
     * @param ean an EAN string from the connections
     * @param profile the name of the profile
     * @param id the name of the identifier matching the profile name
     * @param type the elementtype defined in info.usef.agr.dto.ElementTypeDto
     * @return ElementDto with default values
     * @see info.usef.agr.dto.ElementDto
     * */
    private static ElementDto getElement(String ean, String profile, String id, ElementTypeDto type)
    {
        ElementDto elementDto = new ElementDto();
        elementDto.setProfile(profile);
        elementDto.setDtuDuration(15);
        elementDto.setConnectionEntityAddress(ean);
        elementDto.setId(ean + '.' + id);
        elementDto.setElementType(type);

        for(int idx = 1; idx <= 96; idx++)
        {
            ElementDtuDataDto elementDtuDataDto = new ElementDtuDataDto();
            elementDtuDataDto.setDtuIndex(idx);

            elementDtuDataDto.setProfileUncontrolledLoad(BigInteger.ZERO);

            elementDtuDataDto.setProfileAverageProduction(BigInteger.ZERO);
            elementDtuDataDto.setProfileAverageConsumption(BigInteger.ZERO);

            elementDtuDataDto.setProfilePotentialFlexProduction(BigInteger.ZERO);
            elementDtuDataDto.setProfilePotentialFlexConsumption(BigInteger.ZERO);

            elementDto.getElementDtuData().add( elementDtuDataDto );
        }

        return elementDto;
    }
}
