/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nl.energieprojecthoogdalem.agr.dtos;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class PropositionTest {

    private Proposition all, none, pv, bat;

    @Before
    public void setUp() throws Exception {
        all = new Proposition("y","y");
        bat = new Proposition("n","y");
        pv = new Proposition("y","n");
        none = new Proposition("n","n");
    }

    @Test
    public void testHas() throws Exception {
        checkProposition(all, true ,true);
        checkProposition(bat, false ,true);
        checkProposition(pv, true ,false);
        checkProposition(none, false ,false);
    }

    private void checkProposition(Proposition proposition, boolean hasPv, boolean hasBat)
    {
        Assert.assertEquals(hasPv, proposition.hasPv());
        Assert.assertEquals(hasBat, proposition.hasBattery());
    }

}