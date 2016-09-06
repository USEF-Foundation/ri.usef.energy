/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.messageservice.transportservice.data;

import org.junit.Test;

import static java.math.BigInteger.TEN;

import static org.junit.Assert.*;

public class ActualDeviceDataTest
{
    private ActualDeviceData actualDeviceData;

    private static final String DEVICE = "d";

    @Test
    public void testGetData() throws Exception
    {
        actualDeviceData = new ActualDeviceData(DEVICE, TEN);

        assertEquals(DEVICE, actualDeviceData.getDevice());
        assertEquals(TEN, actualDeviceData.getValue());

    }
}