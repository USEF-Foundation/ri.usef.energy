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

import nl.energieprojecthoogdalem.forecastservice.ConnectionManager;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.*;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AgrConfiguration.class)
public class AgrConfigurationTest
{
    private static final boolean USE_WUNDERGROUND_FILE_BOOL = false;

    private static final int BATTERY_CHARGE_NUMBER = 750
            , BATTERY_FULLYCHARGED_KWH_NUMBER = 1650
            , BATTERY_FULLYCHARGED_KWQ_NUMBER = 1650 *(60 /15)
            ,BATTERY_CHARGE_PERIOD_NUMBER = 8
            ,PTU_DURATION = 15
       ;
    private static double SKY_IRRADIATE_NUMBER = 0.1D;

    private static final String PBC_NAME = "PBC"
                                ,WUNDERGROUND_URL_STR = "http://example.com";

    private static final LocalDate WUNDERGROUND_FILE_PERIOD = new LocalDate("2016-03-19");

    @Spy
    private Properties prop = new Properties();

    @Mock
    private FileReader reader;

    @Mock
    private ConnectionManager manager;

    @Mock
    private PreparedStatement query;

    @Mock
    private Connection connection;

    @Before
    public void init() throws Exception
    {
        PowerMockito.whenNew(FileReader.class).withAnyArguments().thenReturn(reader);

        PowerMockito.whenNew(Properties.class).withAnyArguments().thenReturn(prop);
        PowerMockito.doNothing().when(prop).load(Matchers.any(FileReader.class));

        PowerMockito.whenNew(ConnectionManager.class).withAnyArguments().thenReturn(manager);
        PowerMockito.doReturn(true).when(manager).connect();
        PowerMockito.doReturn(connection).when(manager).getConnection();
        PowerMockito.doNothing().when(manager).disconnect();

        PowerMockito.doReturn(query).when(connection).prepareStatement(Matchers.anyString());
        PowerMockito.doNothing().when(query).setString(Matchers.anyInt(), Matchers.anyString());
    }

    @Test
    public void testLoadDatabaseConfig() throws Exception
    {
        setDatabaseProp();

        Properties result = AgrConfiguration.loadDatabaseConfig();
        assertEquals(prop , result);

        validateDatabaseProp(result);
    }

    @Test
    public void testGetConfig() throws Exception
    {
        setConfigProp();

        Properties result = AgrConfiguration.getConfig(PBC_NAME, WUNDERGROUND_FILE_PERIOD, PTU_DURATION);
        assertEquals(prop , result);

        Mockito.verify(query).setString(1, PBC_NAME);
        Mockito.verify(query).setString(2, ""+WUNDERGROUND_FILE_PERIOD.toString("yyyy-MM-dd"));
        Mockito.verify(query).setString(3, ""+ BATTERY_CHARGE_NUMBER);
        Mockito.verify(query).setString(4, ""+ BATTERY_FULLYCHARGED_KWH_NUMBER);
        Mockito.verify(query).setString(5, ""+USE_WUNDERGROUND_FILE_BOOL);
        Mockito.verify(query).setString(6, ""+SKY_IRRADIATE_NUMBER);

        validateConfigProp(result);
    }

    private void setDatabaseProp()
    {
        prop.setProperty("host","127.0.0.1");
        prop.setProperty("port","1433");
        prop.setProperty("username","root");
        prop.setProperty("password","");
        prop.setProperty("database","master");
        prop.setProperty("timeout","3");
        prop.setProperty("certificate_host","host");
        prop.setProperty("trust_server_certificate","false");
        prop.setProperty("encrypt","true");
    }

    private void validateDatabaseProp(Properties p)
    {
        assertEquals("127.0.0.1", p.getProperty("host"));
        assertEquals("1433", p.getProperty("port"));
        assertEquals("root", p.getProperty("username"));
        assertEquals("", p.getProperty("password"));
        assertEquals("master", p.getProperty("database"));
        assertEquals("3", p.getProperty("timeout"));
        assertEquals("host", p.getProperty("certificate_host"));
        assertEquals("false", p.getProperty("trust_server_certificate"));
        assertEquals("true", p.getProperty("encrypt"));
    }

    private void setConfigProp()
    {
        prop.setProperty(AgrConfiguration.BATTERY_CHARGE, ""+BATTERY_CHARGE_NUMBER);
        prop.setProperty(AgrConfiguration.BATTERY_FULLYCHARGED, ""+ BATTERY_FULLYCHARGED_KWH_NUMBER);
        prop.setProperty(AgrConfiguration.SKY_IRRADIATE, ""+SKY_IRRADIATE_NUMBER);
        prop.setProperty(AgrConfiguration.WUNDERGROUND_FILE_PERIOD, WUNDERGROUND_FILE_PERIOD.toString("yyyy-MM-dd"));
        prop.setProperty(AgrConfiguration.USE_WUNDERGROUND_FILE, ""+ USE_WUNDERGROUND_FILE_BOOL);
        prop.setProperty(AgrConfiguration.WUNDERGROUND_URL, WUNDERGROUND_URL_STR);
    }

    private void validateConfigProp(Properties p)
    {
        assertEquals(""+BATTERY_CHARGE_NUMBER, p.getProperty(AgrConfiguration.BATTERY_CHARGE));
        assertEquals(""+ BATTERY_FULLYCHARGED_KWQ_NUMBER, p.getProperty(AgrConfiguration.BATTERY_FULLYCHARGED));
        assertEquals(""+BATTERY_CHARGE_PERIOD_NUMBER, p.getProperty(AgrConfiguration.BATTERY_CHARGE_DURATION));
        assertEquals(""+SKY_IRRADIATE_NUMBER, p.getProperty(AgrConfiguration.SKY_IRRADIATE));
        assertEquals(WUNDERGROUND_FILE_PERIOD.toString("yyyy-MM-dd"), p.getProperty(AgrConfiguration.WUNDERGROUND_FILE_PERIOD));
        assertEquals(""+ USE_WUNDERGROUND_FILE_BOOL, p.getProperty(AgrConfiguration.USE_WUNDERGROUND_FILE));
        assertEquals(WUNDERGROUND_URL_STR, p.getProperty(AgrConfiguration.WUNDERGROUND_URL));
    }
}