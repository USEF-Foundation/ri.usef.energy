/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.dso.limits;

import info.usef.core.config.AbstractConfig;
import info.usef.pbcfeeder.dto.PbcPowerLimitsDto;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * reads the grid safety limits from a properties file
 * */
public class LimitConfiguration
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LimitConfiguration.class);

    public static String LOWER = "LOWERLIMIT"
                       , UPPER = "UPPERLIMIT"
                    ;

    public static BigDecimal DEFAULT_LOWER = new BigDecimal(-40000)
                        , DEFAULT_UPPER = new BigDecimal(40000)
                    ;

    /**
     * reads the properties from the gridsafetylimits.properties found in the usef-env/localhost/nodes/config/dso
     * used keys are:
     * LOWERLIMIT a number describing the grid safety analysis lower limit
     * UPPERLIMIT a number describing the grid safety analysis upper limit
     *
     * if keys are not valid or found, lower limit -40000 and upper limit 40000 are used
     *
     * @return {@link PbcPowerLimitsDto} with the LOWERLIMIT and UPPERLIMIT set
     * */
    public PbcPowerLimitsDto getLimits(LocalDate period) throws NumberFormatException
    {
        String file = AbstractConfig.getConfigurationFolder() + "gridsafetylimits.properties";
        Properties properties = new Properties();
        try
        {
            properties.load(new FileReader(file));
            validateBigDecimal(properties, LOWER, ""+DEFAULT_LOWER);
            validateBigDecimal(properties, UPPER, ""+DEFAULT_UPPER);
        }
        catch (IOException exception)
        {
            LOGGER.error("Unable to load {} using default lowerlimit {}, upperlimit {}!", file, DEFAULT_UPPER, DEFAULT_LOWER);
            properties.setProperty(LOWER, ""+DEFAULT_LOWER);
            properties.setProperty(UPPER, ""+DEFAULT_UPPER);
        }

        logConfiguration(period, properties);

        return new PbcPowerLimitsDto(new BigDecimal( properties.getProperty(LOWER) ), new BigDecimal( properties.getProperty(UPPER)) );
    }

    /**
     * validates if a property can be parsed to a BigDecimal
     *
     * @param properties the properties list to validate
     * @param key the key name to validate
     * @param defaultValue the default value to use for a key
     * */
    private static void validateBigDecimal(Properties properties, String key, String defaultValue)
    {
        try
        {
            BigDecimal d = new BigDecimal(properties.getProperty(key));
        }
        catch (NullPointerException | NumberFormatException ex)
        {
            LOGGER.error("Unable to parse BigDecimal for key {}", key);
            properties.setProperty(key, defaultValue);
        }
    }

    /**
     * logs the used dso GSA limits to the database
     *
     * @param period the date to log
     * @param prop the configuration to log
     * */
    private static void logConfiguration(LocalDate period, Properties prop)
    {
        ConnectionManager manager = new ConnectionManager();
        if(manager.connect())
        {
            Connection connection = manager.getConnection();
            try
            {
                PreparedStatement query = connection.prepareStatement
                        (
                                "INSERT INTO DSO_CONFIG_HISTORY(period, upperlimit, lowerlimit) " +
                                "VALUES (?, ?, ?);"
                        );

                query.setString(1, period.toString("yyyy-MM-dd"));
                query.setString(2, prop.getProperty(UPPER));
                query.setString(3, prop.getProperty(LOWER));

                query.execute();

                query.close();
                connection.close();
            }
            catch(SQLException | NullPointerException exception)
            {
                LOGGER.error("unable to insert config history in database for GSA, reason ", exception);
            }

            manager.disconnect();
        }
    }
}
