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

/**
 * utility to convert between Hoogdalem home ID and usef EAN
 * */
public final class EANUtil {
    public static String EAN_PREFIX = "ean.800000000000000";

    /**
     * returns ean from number, valid range 0 &#60;-&#62; 999
     *
     * @param home home number in integer format
     * @return  EAN string of home id
     * */
    public static String toEAN(int home)    {   return EAN_PREFIX + String.format("%03d", home);}

    /**
     * returns ean from string, valid range "000" &#60;-&#62; "999"
     *
     * @param home home number in string format
     * @return  EAN string of home id
     * */
    public static String toEAN(String home)
    {
        return EAN_PREFIX + home;
    }

    /**
     * returns home id from ean string
     *
     * @param EAN EAN string of home id
     * @return home number in integer format
     * */
    public static int toHomeInt(String EAN)  {   return Integer.parseInt(EAN.substring(EAN.length() -3));}

    /**
     * returns home id from ean string
     *
     * @param EAN EAN string of home id
     * @return home number in string format
     * */
    public static String toHomeString(String EAN)  {   return EAN.substring(EAN.length() -3);}
}
