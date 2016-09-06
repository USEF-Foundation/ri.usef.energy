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


/**
 * Proposition configuration of a connection
 * */
public class Proposition
{
    private String pv, battery;

    /**
     * Initialize proposition with pv and/or battery
     *
     * @param pv if value is "y" the connection contains solar panels
     * @param battery if value is "y" then the connection contains a battery
     * */
    public Proposition(String pv, String battery)
    {
        this.battery = battery;
        this.pv = pv;
    }

    /**
     * connection has pv
     * @return true if proposition for connection contains pv
     * */
    public boolean hasPv(){return "y".equals(pv);}

    /**
     * connection has battery
     * @return true if proposition for connection contains battery
     * */
    public boolean hasBattery(){return "y".equals(battery);}

}
