/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nl.energieprojecthoogdalem.util;

import org.junit.Assert;
import org.junit.Test;

public class EANUtilTest {

    @Test
    public void testEANUtil() throws Exception {
        int homeNumber = 12;
        String homeString = "012";
        String ean;

        ean = EANUtil.toEAN(homeNumber);
        Assert.assertEquals(EANUtil.EAN_PREFIX + homeString, ean);

        ean = EANUtil.toEAN(homeString);
        Assert.assertEquals(EANUtil.EAN_PREFIX + homeString, ean);

        homeString = EANUtil.toHomeString(ean);
        Assert.assertEquals("012", homeString);

        homeNumber = EANUtil.toHomeInt(ean);
        Assert.assertEquals(12, homeNumber);
    }
}