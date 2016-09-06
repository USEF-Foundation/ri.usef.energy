/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
