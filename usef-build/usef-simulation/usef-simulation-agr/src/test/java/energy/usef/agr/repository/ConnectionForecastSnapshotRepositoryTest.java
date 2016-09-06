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

package energy.usef.agr.repository;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.model.ConnectionForecastSnapshot;
import energy.usef.core.config.AbstractConfig;
import energy.usef.core.util.DateTimeUtil;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigAgr.class, AbstractConfig.class })
public class ConnectionForecastSnapshotRepositoryTest {

    private static final String AGR_IDENTIFY_CHANGE_IN_FORECAST_SERIALIZATION_FILENAME = "connection_forecast_snapshots.json";

    @Mock
    private ConfigAgr configAgr;

    private ConnectionForecastSnapshotRepository repository;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ConfigAgr.class);
        PowerMockito.mockStatic(AbstractConfig.class);
        PowerMockito.when(ConfigAgr.getConfigurationFolder()).thenReturn("src/test/resources/energy/usef/agr/repository/");
        repository = new ConnectionForecastSnapshotRepository();
        Whitebox.setInternalState(repository, configAgr);
    }

    @Test
    public void testFindConnectionForecastSnapshots() {
        List<ConnectionForecastSnapshot> connectionForecastSnapshots = repository.findConnectionForecastSnapshots(
                DateTimeUtil.parseDate("2015-01-01"));
        Assert.assertNotNull(connectionForecastSnapshots);
        Assert.assertEquals(2, connectionForecastSnapshots.size());
    }

    @Test
    public void testFindConnectionForecastSnapshot() {
        ConnectionForecastSnapshot snapshot = repository.findConnectionForecastSnapshot("ean.000000000002",
                new LocalDate("2015-01-02"), 1);
        Assert.assertNotNull(snapshot);
        Assert.assertEquals(1, snapshot.getPtuIndex().intValue());
        Assert.assertEquals("ean.000000000002", snapshot.getConnectionEntityAddress());
        Assert.assertEquals(BigInteger.valueOf(30), snapshot.getPower());
        Assert.assertTrue(snapshot.getChanged());
        Assert.assertEquals(new LocalDate(2015, 1, 2), snapshot.getPtuDate());
    }

    @Test
    public void testFindMissingConnectionForecastSnapshot() {
        String fileName = "connection_forecast_snapshots.json";

        String filePath = ConfigAgr.getConfigurationFolder() + fileName;
        File snapshotsFile = new File(filePath);
        if (snapshotsFile.exists()) {
            snapshotsFile.delete();
        }

        ConnectionForecastSnapshot snapshot = repository.findConnectionForecastSnapshot("ean.000000000002",
                new LocalDate("2015-11-02"), 1);
        Assert.assertNull(snapshot);
    }

    @Test
    public void testSerializeConnectionForecastSnapshots() {
        int amount = 5;
        repository.serializeConnectionForecastSnapshots(buildConnectionForecastsList(amount));

        List<ConnectionForecastSnapshot> newSnapshots = repository.findConnectionForecastSnapshots(
                DateTimeUtil.getCurrentDate());
        Assert.assertEquals(amount, newSnapshots.size());

        // clean up the file
        new File(ConfigAgr.getConfigurationFolder() + DateTimeFormat.forPattern("yyyyMMdd").print(DateTimeUtil.getCurrentDate())
                + "_" + AGR_IDENTIFY_CHANGE_IN_FORECAST_SERIALIZATION_FILENAME).delete();
    }

    @Test
    public void testSerializeSnapshotsWithEmpyInput() {
        try {
            repository.serializeConnectionForecastSnapshots(new ArrayList<>());
        } catch (Exception e) {
            Assert.fail("Exception caught while expecting none.");
        }
    }

    @Test
    public void testSerializeSnapshotsWithNullInput() {
        try {
            repository.serializeConnectionForecastSnapshots(null);
        } catch (Exception e) {
            Assert.fail("Exception caught while expecting none.");
        }
    }

    private List<ConnectionForecastSnapshot> buildConnectionForecastsList(int amount) {
        List<ConnectionForecastSnapshot> connectionForecastSnapshots = new ArrayList<>();
        for (int index = 1; index <= amount; ++index) {
            ConnectionForecastSnapshot connectionForecastSnapshot = new ConnectionForecastSnapshot();
            connectionForecastSnapshot.setPower(BigInteger.valueOf(index));
            connectionForecastSnapshot.setPtuIndex(index);
            connectionForecastSnapshot.setPtuDate(DateTimeUtil.getCurrentDate());
            connectionForecastSnapshot.setChanged(true);
            connectionForecastSnapshot.setConnectionEntityAddress("ean.00000000000" + index);
            connectionForecastSnapshots.add(connectionForecastSnapshot);
        }
        return connectionForecastSnapshots;
    }
}
