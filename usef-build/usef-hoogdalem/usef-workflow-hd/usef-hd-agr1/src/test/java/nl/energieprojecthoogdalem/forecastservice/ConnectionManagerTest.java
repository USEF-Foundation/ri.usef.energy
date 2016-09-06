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

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.Connection;
import java.util.Properties;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ConnectionManager.class)
public class ConnectionManagerTest {

    private ConnectionManager connectionManager;

    private Properties prop;

    @Mock
    private SQLServerDataSource dataSource;

    @Mock
    private Connection connection;

    @Before
    public void setUp() throws Exception {
        prop = new Properties();
        prop.setProperty("host", "localhost");
        prop.setProperty("port", "1444");
        prop.setProperty("username", "test");
        prop.setProperty("password", "abc123");
        prop.setProperty("database", "HoogDalem");
        prop.setProperty("timeout", "2");

        PowerMockito.whenNew(SQLServerDataSource.class).withNoArguments().thenReturn(dataSource);

        connectionManager = new ConnectionManager(prop);

    }

    @Test
    public void testGetConnection() throws Exception
    {
        PowerMockito.when(dataSource.getConnection()).thenReturn(connection);

        assertEquals(true, connectionManager.connect());
        assertEquals(connection, connectionManager.getConnection());
        connectionManager.disconnect();
    }

    @Test
    @SuppressWarnings("unchecked") public void testFailConnection() throws Exception
    {
        PowerMockito.when(dataSource.getConnection()).thenThrow(SQLServerException.class);

        assertEquals(false, connectionManager.connect());
        assertNull(connectionManager.getConnection());
        connectionManager.disconnect();
    }

}