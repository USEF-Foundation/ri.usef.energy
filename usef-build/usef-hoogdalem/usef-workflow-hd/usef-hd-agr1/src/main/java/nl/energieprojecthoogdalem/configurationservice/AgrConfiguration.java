/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.configurationservice;

import info.usef.core.config.AbstractConfig;
import nl.energieprojecthoogdalem.forecastservice.ConnectionManager;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * loads the database properties and the forecast configuration
 * */
public class AgrConfiguration
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrConfiguration.class);

    public static String SKY_IRRADIATE = "SKY_IRRADIATE"
                        ,BATTERY_CHARGE = "BATTERY_CHARGE"
                        ,BATTERY_FULLYCHARGED = "BATTERY_FULLYCHARGED"
                        , BATTERY_CHARGE_DURATION = "BATTERY_CHARGE_DURATION"
                        ,WUNDERGROUND_URL = "WUNDERGROUND_URL"
                        ,USE_WUNDERGROUND_FILE = "USE_WUNDERGROUND_FILE"
                        ,WUNDERGROUND_FILE_PERIOD = "WUNDERGROUND_FILE_PERIOD"
    ;

    /**
     * loads the database.properties file from the configuration folder of the usef party
     * */
    public static Properties loadDatabaseConfig()
    {
        Properties prop = new Properties();
        try
        {
            //LOGGER.trace("config folder: " + AbstractConfig.getConfigurationFolder());
            prop.load(new FileReader(AbstractConfig.getConfigurationFolder() + "database.properties"));
        }
        catch(IOException exception)
        {
            LOGGER.warn("Unable to read database properties reason: {}", exception.getMessage());
        }
        return prop;
    }

    /**
     * logs the variable agr forecast configuration to the database
     *
     * @param pbcName the pbc name that uses the configuration
     * @param period the date to log
     * @param properties the configuration to log
     * */
    private static void logConfiguration(String pbcName, LocalDate period, Properties properties)
    {
        ConnectionManager manager = new ConnectionManager(loadDatabaseConfig());
        if(manager.connect())
        {
            Connection connection = manager.getConnection();

            try
            {
                PreparedStatement query = connection.prepareStatement
                (
                    "INSERT INTO AGR_CONFIG_HISTORY(pbc_name, period, battery_charge, battery_fullycharged, use_wunderground_file, sky_irradiate) " +
                    "VALUES (?, ?, ?, ?, ?, ?);"
                );

                query.setString(1, pbcName);
                query.setString(2, period.toString("yyyy-MM-dd"));
                query.setString(3, properties.getProperty(BATTERY_CHARGE));
                query.setString(4, properties.getProperty(BATTERY_FULLYCHARGED));
                query.setString(5, properties.getProperty(USE_WUNDERGROUND_FILE));
                query.setString(6, properties.getProperty(SKY_IRRADIATE));

                query.execute();

                query.close();
                connection.close();
            }
            catch(SQLException | NullPointerException exception)
            {
                LOGGER.error("unable to insert config history in database for {}, reason ", pbcName, exception);
            }

            manager.disconnect();
        }
        else
            LOGGER.error("Unable to connect to the TSQL database configuration will not be logged.");
    }

    /**
    * retrieves the variable AGR configuration
    *
    * @param pbcName the pbc name that uses the configuration
    * @param period the date to log
    * @param ptuDuration the duration of one ptu
    * */
    public static Properties getConfig(String pbcName, LocalDate period, int ptuDuration)
    {
        String config = AbstractConfig.getConfigurationFolder() + "agr_configuration.properties";
        Properties prop = new Properties();

        try
        {
            prop.load( new FileReader(config) );

            verifySkyFactor(prop);

            verifyNumber(prop, BATTERY_CHARGE, "750");
            verifyNumber(prop, BATTERY_FULLYCHARGED, "1650");

            verifyBool(prop, USE_WUNDERGROUND_FILE, "false");

            verifyDate(prop, WUNDERGROUND_FILE_PERIOD, "2016-03-19");

        }
        catch (IOException exception)
        {
            LOGGER.warn("Unable to read agr properties reason: {}", exception.getMessage());
            prop.setProperty(SKY_IRRADIATE, "0.9");
            prop.setProperty(BATTERY_CHARGE, "750");
            prop.setProperty(BATTERY_FULLYCHARGED, "1650");
            prop.setProperty(BATTERY_CHARGE_DURATION, "8");
            prop.setProperty(USE_WUNDERGROUND_FILE, "false");
            prop.setProperty(WUNDERGROUND_FILE_PERIOD, "2016-03-19");
            prop.setProperty(WUNDERGROUND_URL, "undefined");
        }

        logConfiguration(pbcName, period, prop);
        calculateBatteryPeriod(prop, ptuDuration);

        return prop;
    }

    /**
     * calculates the number of ptus when the battery is fully charged
     * calculates from watt / hour to watt / ptuDuration
     *
     * @param prop the configuration to use for calculation
     * @param ptuDuration the duration of one ptu
     * */
    private static void calculateBatteryPeriod(Properties prop, int ptuDuration)
    {
        try
        {
            int batteryCharge = Integer.parseInt(prop.getProperty(BATTERY_CHARGE, "750"))
                    , batteryFullyCharged = Integer.parseInt(prop.getProperty(BATTERY_FULLYCHARGED, "1650") ) * (60 / ptuDuration);

            prop.setProperty(BATTERY_FULLYCHARGED, ""+batteryFullyCharged);
            prop.setProperty(BATTERY_CHARGE_DURATION,  ""+batteryFullyCharged / batteryCharge );
        }
        catch (NumberFormatException | NullPointerException exception)
        {
            LOGGER.error("unable to calculate {} reason", BATTERY_CHARGE_DURATION, exception);
            prop.setProperty(BATTERY_CHARGE_DURATION, "8");
        }
    }

    /**
     * verifies if the property can be parsed to a double and is between 0 and 1
     *
     * @param prop the properties to verify
     * */
    private static void verifySkyFactor(Properties prop)
    {
        try
        {
            double skyFactor = Double.parseDouble(prop.getProperty(AgrConfiguration.SKY_IRRADIATE));
            if(skyFactor > 1D || skyFactor < 0D)
            {
                LOGGER.warn("Sky Factor out of bounds");
                prop.setProperty(SKY_IRRADIATE, "0.9");
            }
        }
        catch (NullPointerException | NumberFormatException exception)
        {
            LOGGER.error("Unable to parse {} reason", AgrConfiguration.SKY_IRRADIATE, exception);
            prop.setProperty(SKY_IRRADIATE, "0.9");
        }
    }

    /**
     * validates if a property can be parsed to a Integer
     *
     * @param prop the properties list to validate
     * @param key the key name to validate
     * @param def the default value to use for a key
     * */
    private static void verifyNumber(Properties prop, String key, String def)
    {
        try
        {
            BigInteger t = new BigInteger(prop.getProperty(key));
        }
        catch (NumberFormatException ex)
        {
            LOGGER.error("Unable to parse number for key {}", key);
            prop.setProperty(key, def);
        }

    }

    /**
     * validates if a property can be parsed to a Boolean
     *
     * @param prop the properties list to validate
     * @param key the key name to validate
     * @param def the default value to use for a key
     * */
    private static void verifyBool(Properties prop, String key, String def)
    {
        try
        {
            boolean bool = Boolean.parseBoolean(prop.getProperty(key));
        }
        catch (NumberFormatException ex)
        {
            LOGGER.error("Unable to parse boolean for key {}", key);
            prop.setProperty(key, def);
        }

    }

    /**
     * validates if a property can be parsed to a LocalDate
     *
     * @param prop the properties list to validate
     * @param key the key name to validate
     * @param def the default value to use for a key
     * */
    private static void verifyDate(Properties prop, String key, String def)
    {
        try
        {
            LocalDate date = new LocalDate(prop.getProperty(key));
        }
        catch (IllegalArgumentException ex)
        {
            LOGGER.error("Unable to parse boolean for key {}", key);
            prop.setProperty(key, def);
        }
    }
}
