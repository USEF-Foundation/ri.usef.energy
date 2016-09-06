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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import info.usef.agr.dto.UdiPortfolioDto;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import org.junit.Test;

import static org.junit.Assert.*;

public class PortfolioUtilTest
{
    private static final int CONNECTION_COUNT = 24;

    @Test
    public void testPortfolioUtil() throws Exception
    {
        List<ConnectionPortfolioDto> allConnections = buildConnectionPortfolioDtos();
        List<ConnectionPortfolioDto> zihConnections = new ArrayList<>();
        List<ConnectionPortfolioDto> nodConnections = new ArrayList<>();

        PortfolioUtil.splitZIHNOD(allConnections, zihConnections, nodConnections);

        assertEquals(CONNECTION_COUNT - CONNECTION_COUNT/2, zihConnections.size());
        assertEquals(CONNECTION_COUNT/2, nodConnections.size());

        PortfolioUtil.joinZIHNOD(allConnections, zihConnections, nodConnections);

        assertEquals(CONNECTION_COUNT, allConnections.size());
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolioDtos()
    {
        return IntStream.rangeClosed(1, CONNECTION_COUNT)
                .mapToObj(index ->
                {
                    ConnectionPortfolioDto connection = new ConnectionPortfolioDto(EANUtil.EAN_PREFIX + index);
                    connection.getUdis().add(buildUdis(index));
                    return connection;
                })
                .collect(Collectors.toList());
    }

    private UdiPortfolioDto buildUdis(int idx)
    {
        if(idx <= CONNECTION_COUNT/2)
            return new UdiPortfolioDto("", 96, ElementType.BATTERY_NOD);

        else
           return new UdiPortfolioDto("", 96, ElementType.BATTERY_ZIH);
    }
}