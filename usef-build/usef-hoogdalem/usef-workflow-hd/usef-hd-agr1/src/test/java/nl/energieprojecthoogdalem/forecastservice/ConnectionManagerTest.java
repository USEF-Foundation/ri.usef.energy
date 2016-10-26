/*
 * Copyright 2015-2016 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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