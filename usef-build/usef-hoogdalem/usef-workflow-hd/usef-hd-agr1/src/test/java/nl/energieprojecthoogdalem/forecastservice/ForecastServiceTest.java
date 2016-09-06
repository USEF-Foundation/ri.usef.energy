/**
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nl.energieprojecthoogdalem.forecastservice;

import nl.energieprojecthoogdalem.agr.dtos.Proposition;
import nl.energieprojecthoogdalem.configurationservice.AgrConfiguration;
import nl.energieprojecthoogdalem.util.EANUtil;
import nl.energieprojecthoogdalem.util.TimeUtil;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.*;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ForecastService.class, AgrConfiguration.class})
public class ForecastServiceTest
{

    private ForecastService forecastService;

    private static final int PTU_COUNT = 96
                            ,PTU_DURATION = 15
                        ;

    private static final LocalDate PERIOD = new LocalDate(2016, 5, 5);

    @Mock
    private Properties properties;

    @Mock
    private AgrConfiguration configurator;

    @Mock
    private ConnectionManager connectionManager;

    @Mock
    private Connection connection;

    @Mock
    private CallableStatement statement;

    @Mock
    private ResultSet resultSet;

    @Before
    public  void setup() throws Exception
    {
        PowerMockito.mockStatic(AgrConfiguration.class);

        Mockito.doNothing().when(connectionManager).disconnect();
        Mockito.when(connectionManager.connect()).thenReturn(true);
        Mockito.when(connectionManager.getConnection()).thenReturn(connection);

        Mockito.when(connection.prepareCall(Matchers.anyString(), Matchers.anyInt(), Matchers.anyInt() ) ).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);

        Mockito.doNothing().when(statement).setString(Matchers.anyInt(), Matchers.anyString());
        Mockito.doNothing().when(statement).setInt(Matchers.anyInt(), Matchers.anyInt());

        PowerMockito.when(AgrConfiguration.loadDatabaseConfig()).thenReturn(properties);
        PowerMockito.whenNew(ConnectionManager.class).withAnyArguments().thenReturn(connectionManager);

        //Whitebox.setInternalState(forecastService, "connectionManager", connectionManager);


    }

    @After
    public void teardown()
    {
        forecastService.disconnect();
    }

    @Test
    public void retrieveProposition() throws Exception
    {
        forecastService = new ForecastService();
        forecastService.connect();

        Mockito.doNothing().when(statement).setString(Matchers.anyInt(), Matchers.anyString());

        Mockito.when(resultSet.next()).thenReturn(true);
        Mockito.when(resultSet.getString( Matchers.matches("Battery") )).thenReturn("y");
        Mockito.when(resultSet.getString( Matchers.matches("PV") )).thenReturn("n");

        Proposition result = forecastService.retrieveProposition("031");

        assertEquals(true, result.hasBattery());
        assertEquals(false, result.hasPv());
    }

    @Test
    public void retrieveForecast() throws Exception
    {
        forecastService = new ForecastService();
        forecastService.connect();

        Mockito.when(resultSet.next()).thenReturn( true,true,false );

        Mockito.when(resultSet.getString( Matchers.matches("Time") )).thenReturn("00:00", "00:15");
        Mockito.when(resultSet.getLong( Matchers.matches("Watt") )).thenReturn( 5L, 10L );//*idx

        Map<Integer ,Long> dtos = forecastService.retrieveForecast(PERIOD, PTU_DURATION);

        assertEquals(96, dtos.size());

        assertEquals(Long.valueOf(5L ), dtos.get(1) );
        assertEquals(Long.valueOf(10L ), dtos.get(2) );

        for(int i = 3; i <= PTU_COUNT; i++)
            assertEquals(Long.valueOf(0L), dtos.get(i));
    }

    @Test
    public void retrievePVForecast() throws Exception
    {
        forecastService = new ForecastService();
        forecastService.connect();

        Long first = 8L
            , next = 14L
                ;

        Mockito.when(resultSet.next()).thenReturn( true, buildQueryResultNumber());
        Mockito.when(resultSet.getString( Matchers.matches("Time") )).thenReturn("00:00", buildTimeQueryResult());

        Long[] l = buildWattQueryResult(next);

        Mockito.when(resultSet.getLong( Matchers.matches("Watt") )).thenReturn(first, l);//*idx

        String ean = EANUtil.EAN_PREFIX + "031";
        LocalDate period = LocalDate.fromDateFields(Date.valueOf("2015-08-31"));

        Map<Integer ,Long> dtos = forecastService.retrievePVForecast(EANUtil.toHomeString(ean), period, PTU_DURATION);

        assertEquals(96, dtos.size());

        assertEquals(first, dtos.get(1) );

        for(int i = 2; i <= PTU_COUNT; i++)
            assertEquals(next, dtos.get(i) );

    }

    private Boolean[] buildQueryResultNumber()
    {
        Boolean[] b = new Boolean[PTU_COUNT];

        for(int i = 0; i < PTU_COUNT; b[i++] = i < PTU_COUNT);

        return b;
    }

    private String[] buildTimeQueryResult()
    {
        int len = PTU_COUNT -1;

        String[] s = new String[len];

        for(int i = 0; i < len; s[i++] = TimeUtil.getTimeFromPtu(i+1 ,PTU_DURATION ));

        return s;

    }

    private Long[] buildWattQueryResult(long val)
    {
        Long[] l = new Long[PTU_COUNT];

        for(int i = 0; i < PTU_COUNT; l[i++] = val);

        return l;
    }

}