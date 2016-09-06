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

public final class ElementType
{
    public static final String PV = "PV"
                            ,NOD = "NOD"
                            ,ZIH = "ZIH"
                            ,BWZ = "BWZ"
                            ,BATTERY = "BATTERY"
                            ,BATTERY_ID = BATTERY +1
                            ,BATTERY_NOD = BATTERY + '_' + NOD
                            ,BATTERY_NOD_ID = BATTERY_NOD +1
                            ,BATTERY_ZIH = BATTERY + '_' + ZIH
                            ,BATTERY_ZIH_ID = BATTERY_ZIH +1
                            ,BATTERY_BWZ = BATTERY + '_' + BWZ
                            ,BATTERY_BWZ_ID = BATTERY_BWZ +1
                            ,HOME = "HOME"
                            ,HOME_ID = HOME +1
                        ;
}
