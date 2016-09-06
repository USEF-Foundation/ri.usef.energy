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

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.UdiPortfolioDto;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;

import java.util.Iterator;
import java.util.List;

/**
 * utility for lists of {@link ConnectionPortfolioDto}
 * */
public final class PortfolioUtil
{
    /**
     * splits all the connections in 2 seperate lists
     * @param allConnections the total connections to split
     * @param zihConnections the connections containing ZIH propositions
     * @param nodConnections the connections containing NOD propositions
     * */
    public static void splitZIHNOD(List<ConnectionPortfolioDto> allConnections, List<ConnectionPortfolioDto> zihConnections, List<ConnectionPortfolioDto> nodConnections )
    {
        Iterator<ConnectionPortfolioDto> connectionIterator = allConnections.iterator();
        while(connectionIterator.hasNext())
        {
            ConnectionPortfolioDto connection = connectionIterator.next();
            String proposition = getProposition(connection.getUdis());
            switch(proposition)
            {
                case ElementType.ZIH:
                    zihConnections.add(connection);
                    connectionIterator.remove();
                    break;
                case ElementType.NOD:
                    nodConnections.add(connection);
                    connectionIterator.remove();
                    break;
            }
        }
    }

    /**
     * joins 2 {@link ConnectionPortfolioDto} lists in one list
     * @param allConnections a list to join all {@link ConnectionPortfolioDto} in
     * @param zihConnections a list containing ZIH {@link ConnectionPortfolioDto}'s
     * @param nodConnections a list containing NOD {@link ConnectionPortfolioDto}'s
     * */
    public static void joinZIHNOD(List<ConnectionPortfolioDto> allConnections, List<ConnectionPortfolioDto> zihConnections, List<ConnectionPortfolioDto> nodConnections )
    {
        allConnections.addAll(zihConnections);
        allConnections.addAll(nodConnections);
    }

    /**
     * looks for the proposition in a list of udis
     * @param udis a list of {@link UdiPortfolioDto} to determine the proposition for
     * @return the the determined proposition, null if the proposition cannot be determined
     * */
    private static String getProposition(List<UdiPortfolioDto> udis)
    {
        String proposition = null;
        for(UdiPortfolioDto udi : udis)
        {
            String profile = udi.getProfile();
            if(ElementType.BATTERY_ZIH.equals(profile))
            {
                proposition = ElementType.ZIH;
                break;
            }
            else if(ElementType.BATTERY_NOD.equals(profile))
            {
                proposition = ElementType.NOD;
                break;
            }

        }
        return proposition;
    }
}
