/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.forecastservice.forecast;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.core.util.PtuUtil;
import org.joda.time.LocalDate;

import java.math.BigInteger;
/**
 * default value class for USEF forecasts per connection
 * */
public final class DefaultForecast
{

    /**
     * returns an {@link ConnectionPortfolioDto} for a connection
     * with the connection and udi(s) forecasts set to 0
     * @param date the date of the forecast
     * @param ptuDuration the duration of one ptu
     * @param connection the connection to add zero 'ed forecasts for
     * @return a {@link ConnectionPortfolioDto} with forecasts containing 0 values
     * */
    public static ConnectionPortfolioDto getDefaultConnectionPortfolio(LocalDate date, int ptuDuration, ConnectionPortfolioDto connection)
    {
        int ptuCount = PtuUtil.getNumberOfPtusPerDay(date, ptuDuration);

        for (int i = 1; i <= ptuCount; i++)
        {
            PowerContainerDto powerContainerDto = buildPowerContainerDto(date, i);

            connection.getConnectionPowerPerPTU().put(i, powerContainerDto);
            for (UdiPortfolioDto udi : connection.getUdis())
                udi.getUdiPowerPerDTU().put(i, powerContainerDto);
        }

        return connection;
    }

    /**
     * returns an {@link PowerContainerDto} with everything set to 0
     * @param date the date of the forecast
     * @param idx the ptu index of the forecast
     * @return a {@link PowerContainerDto} containing 0 values
     * */
    private static PowerContainerDto buildPowerContainerDto(LocalDate date, int idx)
    {
        PowerContainerDto powerContainerDto = new PowerContainerDto(date, idx);
        powerContainerDto.getForecast().setUncontrolledLoad(BigInteger.ZERO);

        powerContainerDto.getForecast().setAverageConsumption(BigInteger.ZERO);
        powerContainerDto.getForecast().setAverageProduction(BigInteger.ZERO);

        powerContainerDto.getForecast().setPotentialFlexConsumption(BigInteger.ZERO);
        powerContainerDto.getForecast().setPotentialFlexProduction(BigInteger.ZERO);

        return powerContainerDto;
    }
}