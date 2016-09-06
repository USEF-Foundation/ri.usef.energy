/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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