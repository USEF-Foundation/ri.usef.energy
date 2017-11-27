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

package nl.energieprojecthoogdalem.dso.limits;

import info.usef.pbcfeeder.dto.PbcPowerLimitsDto;
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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LimitConfiguration.class)
public class LimitConfigurationTest
{
    private static final BigDecimal LOWER_VAL = BigDecimal.valueOf(-3000)
                                    ,UPPER_VAL = BigDecimal.valueOf(40000)
            ;

    private static final LocalDate PERIOD = new LocalDate("2016-04-28");

    private LimitConfiguration limitConfiguration;

    @Spy
    private Properties properties = new Properties();

    @Mock
    private ConnectionManager connectionManager;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement query;

    @Mock
    private FileReader reader;

    @Before
    public void init() throws Exception
    {
        limitConfiguration = new LimitConfiguration();


        properties.setProperty(LimitConfiguration.LOWER, ""+LOWER_VAL);

        PowerMockito.whenNew(FileReader.class).withAnyArguments().thenReturn(reader);
        PowerMockito.whenNew(Properties.class).withNoArguments().thenReturn(properties);
        PowerMockito.doNothing().when(properties).load(Matchers.any(FileReader.class));

        PowerMockito.whenNew(ConnectionManager.class).withAnyArguments().thenReturn(connectionManager);
        PowerMockito.when(connectionManager.connect()).thenReturn(true);
        PowerMockito.when(connectionManager.getConnection()).thenReturn(connection);
        PowerMockito.doNothing().when(connectionManager).disconnect();

        PowerMockito.doNothing().when(connection).close();
        PowerMockito.when(connection.prepareStatement(Matchers.anyString())).thenReturn(query);
        PowerMockito.doNothing().when(query).setString(Matchers.anyInt(), Matchers.anyString());
        PowerMockito.when(query.execute()).thenReturn(false);
        PowerMockito.doNothing().when(query).close();
    }

    @Test
    public void testGetMissingLimits() throws Exception
    {
        PbcPowerLimitsDto result = limitConfiguration.getLimits(PERIOD);
        assertEquals(LOWER_VAL, result.getLowerLimit());
        assertEquals(UPPER_VAL, result.getUpperLimit());

        Mockito.verify(query).setString(1, PERIOD.toString("yyyy-MM-dd"));
        Mockito.verify(query).setString(2, ""+UPPER_VAL);
        Mockito.verify(query).setString(3, ""+LOWER_VAL);
    }

    @Test
    public void testGetDefaultLimits() throws Exception
    {
        PowerMockito.doThrow(new IOException()).when(properties).load(Matchers.any(Reader.class));

        PbcPowerLimitsDto result = limitConfiguration.getLimits(PERIOD);
        assertEquals(LimitConfiguration.DEFAULT_LOWER, result.getLowerLimit());
        assertEquals(LimitConfiguration.DEFAULT_UPPER, result.getUpperLimit());
    }
}