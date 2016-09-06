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

package energy.usef.core.workflow.transformer;

import energy.usef.core.data.xml.bean.message.ConnectionMeterData;
import energy.usef.core.data.xml.bean.message.MeterData;
import energy.usef.core.data.xml.bean.message.MeterDataSet;
import energy.usef.core.data.xml.bean.message.PTUMeterData;
import energy.usef.core.workflow.dto.MeterDataSetDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for the unit tests related to the {@link MeterDataTransformer} class.
 */
public class MeterDataTransformerTest {

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, MeterDataTransformer.class.getDeclaredConstructors().length);
        Constructor<MeterDataTransformer> constructor = MeterDataTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransform() throws Exception {
        List<MeterDataSet> meterDataSets = buildMeterDataSets();
        List<MeterDataSetDto> meterDataSetDtos = MeterDataTransformer.transform(meterDataSets);
        Assert.assertNotNull(meterDataSetDtos);
        Assert.assertEquals(2, meterDataSetDtos.size());
        Assert.assertEquals(1, meterDataSetDtos.get(0).getMeterDataDtos().size());
        Assert.assertEquals(1, meterDataSetDtos.get(0).getMeterDataDtos().get(0).getConnectionMeterDataDtos().size());
        Assert.assertEquals(96,
                meterDataSetDtos.get(0).getMeterDataDtos().get(0).getConnectionMeterDataDtos().get(0).getPtuMeterDataDtos().size());
    }

    private List<MeterDataSet> buildMeterDataSets() {
        PTUMeterData ptus1 = buildPtuMeterData(1L, 96L, 1000L);
        PTUMeterData ptus2 = buildPtuMeterData(1L, 96L, 500L);
        ConnectionMeterData connectionMeterData1 = buildConnectionMeterData("agr1.usef-example.com", 2);
        ConnectionMeterData connectionMeterData2 = buildConnectionMeterData("agr2.usef-example.com", 2);
        connectionMeterData1.getPTUMeterData().add(ptus1);
        connectionMeterData2.getPTUMeterData().add(ptus2);
        MeterData meterData1 = buildMeterData(new LocalDate());
        MeterData meterData2 = buildMeterData(new LocalDate());
        meterData1.getConnectionMeterData().add(connectionMeterData1);
        meterData2.getConnectionMeterData().add(connectionMeterData2);
        MeterDataSet set1 = buildMeterDataSet("ean.000000000001");
        MeterDataSet set2 = buildMeterDataSet("ean.000000000002");
        set1.getMeterData().add(meterData1);
        set2.getMeterData().add(meterData2);
        return Arrays.asList(set1, set2);
    }

    private MeterDataSet buildMeterDataSet(String usefIdentifier) {
        MeterDataSet meterDataSet = new MeterDataSet();
        meterDataSet.setEntityAddress(usefIdentifier);
        return meterDataSet;
    }

    private MeterData buildMeterData(LocalDate period) {
        MeterData meterData = new MeterData();
        meterData.setPeriod(period);
        return meterData;
    }

    private ConnectionMeterData buildConnectionMeterData(String entityAddress) {
        ConnectionMeterData connectionMeterData = new ConnectionMeterData();
        connectionMeterData.setEntityAddress(entityAddress);
        return connectionMeterData;
    }

    private ConnectionMeterData buildConnectionMeterData(String agrDomain, Integer connectionCount) {
        ConnectionMeterData connectionMeterData = new ConnectionMeterData();
        connectionMeterData.setAGRDomain(agrDomain);
        connectionMeterData.setEntityCount(BigInteger.valueOf(connectionCount));
        return connectionMeterData;
    }

    private PTUMeterData buildPtuMeterData(Long start, Long duration, Long power) {
        PTUMeterData ptuMeterData = new PTUMeterData();
        ptuMeterData.setStart(BigInteger.valueOf(start));
        ptuMeterData.setDuration(BigInteger.valueOf(duration));
        ptuMeterData.setPower(BigInteger.valueOf(power));
        return ptuMeterData;
    }
}
