/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nl.energieprojecthoogdalem.forecastservice;

import info.usef.core.util.PtuUtil;

import nl.energieprojecthoogdalem.agr.dtos.Proposition;
import nl.energieprojecthoogdalem.configurationservice.AgrConfiguration;
import nl.energieprojecthoogdalem.util.TimeUtil;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

/**
 * usef forecast feeder service for the ms sqlserver database
 * */
public class ForecastService
{
    private Logger LOGGER = LoggerFactory.getLogger(ForecastService.class);
    private ConnectionManager connectionManager;

    /**
     * database service initializer,
     * initializes the database using the database.properties located in the usef AbstractConfig.getConfigurationFolder()
     *
     * for items in properties see {@link ConnectionManager}
     *
     * @see info.usef.core.config.AbstractConfig
     * @see nl.energieprojecthoogdalem.forecastservice.ConnectionManager
     * */
    public ForecastService()
    {
        connectionManager = new ConnectionManager(AgrConfiguration.loadDatabaseConfig());
    }

    /**
     * wrapped function of the ConnectionManager.disconnect()
     * @see ConnectionManager
     * */
    public void disconnect()    {   connectionManager.disconnect(); }

    /**
     * wrapped function of the ConnectionManager.connect()
     * @return a java.sql.Connection
     * @see ConnectionManager
     * */
    public boolean connect()    {   return connectionManager.connect();}

    /**
     * configuration of a home
     *
     * uses the stored procedure uspDetermineDevices with (VARCHAR) parameter home id
     * expects (varchar) coloumn "PV" and (varchar) coloumn "Battery"
     *
     * @param homeId the id from a home
     *
     * @return a Proposition class for the specified homeid
     * @see Proposition
     * */
    public Proposition retrieveProposition(String homeId)
    {
        Connection connection = connectionManager.getConnection();

        if(connection == null)
            return null;

        try
        {
            CallableStatement statement = connection.prepareCall("{call uspDetermineDevices(?)}",ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            statement.setString(1, homeId);
            //
            Proposition proposition = null;
            ResultSet result = statement.executeQuery();
            //
            if(result.next())
                proposition = new Proposition(result.getString("PV"), result.getString("Battery"));

            else
                LOGGER.error("no result for proposition of home ID {}", homeId);

            //statement / result .close in finally will get "variable might not been initialized and throws an additional SQLException"
            result.close();
            statement.close();

            return proposition;
        }
        catch(SQLException exception )
        {
            LOGGER.error("Unable to prepare/execute call for uspDetermineDevices() reason: ", exception);
            return null;
        }
        catch(NullPointerException exception)
        {
            LOGGER.error("uncaught nullpointer while executes call uspDetermineDevices( {} ) stack: ", homeId,  exception);
            return null;
        }

    }

    /**
     * general forecast data of uncontrolled load
     *
     *  uses the stored procedure uspDetermineForecastGMT1 with (VARCHAR|Date) parameter date
     *  expects (varchar) coloumn(s) "Time" and (INT) coloumn(s) "Watt"
     *
     * @param period a org.joda.time.LocalDate object containing the date for the forecast
     * @param ptuDuration the duration of one ptu in minutes (15)
     *
     * @return a HashMap Integer, Long with Integer keys from 1 - 96 containing Long forecast values in watt
     * @see org.joda.time.LocalDate
     * */
    public Map<Integer, Long> retrieveForecast(LocalDate period, int ptuDuration)
    {
        Map<Integer, Long> dtos = defaultMap(PtuUtil.getNumberOfPtusPerDay(period, ptuDuration));
        String date = String.format("%d-%02d-%02d", period.getYear(),period.getMonthOfYear(), period.getDayOfMonth());
        LOGGER.debug("date: " + date);

        Connection connection = connectionManager.getConnection();

        if(connection == null)
            return dtos;

        try
        {
            CallableStatement statement = connection.prepareCall("{call uspDetermineForecastGMT1(?)}",ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            statement.setString(1, date);
            //
            ResultSet result = statement.executeQuery();
            boolean hasResult = false;
            //
            while(result.next())
            {
                hasResult = true;

                dtos.put( PtuUtil.getPtuIndex(TimeUtil.getLocalDateTimeFromDateTimeString(date + ' ' + result.getString("Time")),ptuDuration)
                        , result.getLong("Watt") );
            }
            if(!hasResult)
                LOGGER.error("no result for uspDetermineForecastGMT1( {} )", date);


            result.close();
            statement.close();

        }
        catch(SQLException exception ) //  SQL + NullPointer
        {
            LOGGER.error("Unable to prepare/execute call for uspDetermineForecastGMT1( {} ) reason: ", date, exception);
        }
        catch(NullPointerException exception)
        {
            //possible due to LocalDateTime being null, should also throw an error
            LOGGER.error("uncaught nullpointer while executing call uspDetermineForecastGMT1( {}, {} ) stack: ", date,  exception);
        }
        return dtos;
    }

    /**
     * pv production forecast data specific for a home
     *
     *  uses the stored procedure uspDeterminePVForecast with (VARCHAR) parameter home id and (INT) parameter month
     *  expects (varchar) coloumn(s) "Time" and (INT) coloumn(s) "Watt"
     *
     * @param homeId the id from a home
     * @param period a org.joda.time.LocalDate object containing the date for the forecast
     * @param ptuDuration the duration of one ptu in minutes (15)
     *
     * @return a HashMap with Integer keys from 1 - 96 containing Long pv forecast values in watt
     * @see org.joda.time.LocalDate
     * */
    public Map<Integer, Long> retrievePVForecast(String homeId, LocalDate period, int ptuDuration )
    {
        int month = period.getMonthOfYear();
        Map<Integer, Long> dtos = defaultMap(PtuUtil.getNumberOfPtusPerDay(period, ptuDuration));

        Connection connection = connectionManager.getConnection();

        if(connection == null)
            return dtos;

        try
        {
            CallableStatement statement = connection.prepareCall("{call uspDeterminePVForecast(?, ?)}",ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            statement.setString(1, homeId);
            statement.setInt(2, month);
            //
            ResultSet result = statement.executeQuery();

            boolean hasResult = false;
            String date = String.format("%d-%02d-%02d", period.getYear(), month, period.getDayOfMonth());
            //
            while(result.next())
            {
                hasResult = true;
                dtos.put( PtuUtil.getPtuIndex(TimeUtil.getLocalDateTimeFromDateTimeString(date + ' ' + result.getString("Time")) ,ptuDuration), result.getLong("Watt") );
            }

            if(!hasResult)
                LOGGER.error("no result for uspDeterminePVForecast( {}, {} )", homeId, month );

            //statement / result .close in finally will get "variable might not been initialized and throws an additional SQLException"
            result.close();
            statement.close();

        }
        catch(SQLException exception ) //  SQL + NullPointer
        {
            LOGGER.error("Unable to prepare/execute call uspDeterminePVForecast( {}, {} ) reason: ", homeId, month,  exception);
        }
        catch(NullPointerException exception)
        {
            //possible due to LocalDateTime being null, should also throw an error
            LOGGER.error("uncaught nullpointer while executing call uspDeterminePVForecast( {}, {} ) stack: ", homeId, month,  exception);
        }

        return dtos;
    }

    /**
     * returns a map with 0L values, size determined by ptu_count
     * @param ptu_count number of values to be set, is equal to number of ptus in a day
     * @return a Map<Integer, Long> with Long values set to 0L
     * */
    private Map<Integer, Long> defaultMap(int ptu_count)
    {
        Map<Integer, Long> defaultMap = new HashMap<>();

        for(int i = 1; i <= ptu_count; defaultMap.put(i++, 0L));

        return defaultMap;
    }
}
