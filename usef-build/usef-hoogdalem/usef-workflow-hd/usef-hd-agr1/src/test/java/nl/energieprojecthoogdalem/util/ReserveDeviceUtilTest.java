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

package nl.energieprojecthoogdalem.util;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.core.config.AbstractConfig;
import nl.energieprojecthoogdalem.agr.devicemessages.ReservedDevice;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
public class ReserveDeviceUtilTest
{
    private static final int DEVICE_COUNT = 5
                            ,SHIFTED_IDX = 60
    ;
    private static final String RESERVED_MESSAGES_FILE = AbstractConfig.getConfigurationFolder() + "reserved_messages.json"
                            ,DEVICE_PREFIX = "device"
                            , PERIOD_STRING_FORMAT = "yyyy-MM-dd"
            ;

    private static final File reserved = new File(RESERVED_MESSAGES_FILE);

    private static final LocalDate PERIOD = new LocalDate(2016, 3, 30);

    @Test
    public void testReadWriteReservationFile() throws Exception
    {
        long seq1 = 50, seq2 = 10;

        String key1 = ""+ seq1
              ,key2 = ""+ seq2;

        //BEFORE
        deleteFile();

        Map<String, ReservedDevice> result2, result1 = ReserveDeviceUtil.readReservation(""+0);
        assertTrue(result1.isEmpty());

        ReserveDeviceUtil.writeReservation(key1, buildDevices(seq1));

        result1 = ReserveDeviceUtil.readReservation(key1);
        assertFalse(result1.isEmpty());

        for(int i  = 1; i <= DEVICE_COUNT; i++)
        {
            validateDevice(result1.get(DEVICE_PREFIX+i), i, seq1);
        }

        ReserveDeviceUtil.writeReservation(key2, buildDevices(seq2));

        result1 = ReserveDeviceUtil.readReservation(key1);
        assertFalse(result1.isEmpty());

        result2 = ReserveDeviceUtil.readReservation(key2);
        assertFalse(result2.isEmpty());

        for(int i  = 1; i <= DEVICE_COUNT; i++)
        {
            validateDevice(result1.get(DEVICE_PREFIX+i) ,i, seq1);
            validateDevice(result2.get(DEVICE_PREFIX+i) ,i, seq2);
        }

        ReserveDeviceUtil.deleteReservation(key1);

        result1 = ReserveDeviceUtil.readReservation(key1);
        assertTrue(result1.isEmpty());

        result2 = ReserveDeviceUtil.readReservation(key2);
        assertFalse(result2.isEmpty());

        for(int i  = 1; i <= DEVICE_COUNT; i++)
        {
            validateDevice(result2.get(DEVICE_PREFIX+i) ,i, seq2);
        }

        ReserveDeviceUtil.writeReservation(key2, buildDevices(seq2, DEVICE_COUNT));

        result2 = ReserveDeviceUtil.readReservation(key2);
        assertFalse(result2.isEmpty());

        for(int i = 1; i <= DEVICE_COUNT; i++)
        {
            assertNull(result2.get(DEVICE_PREFIX+i));
        }

        for(int i = 1 +DEVICE_COUNT; i <= DEVICE_COUNT*2; i++)
        {
            validateDevice(result2.get(DEVICE_PREFIX+i) ,i, seq2);
        }

        //AFTER
        ReserveDeviceUtil.deleteReservationFile();
        assertFalse(reserved.exists());
    }

    @Test
    public void testCreateReservationsMap() throws Exception
    {
        int connectionSize = 8;

        Map<String, ReservedDevice> result = new HashMap<>();

        ReserveDeviceUtil.reserveDeviceMessages(result, buildConnections(connectionSize, ElementType.BATTERY_ZIH), buildShiftedIndexes(DEVICE_COUNT, SHIFTED_IDX), PERIOD, ElementType.BATTERY_ZIH);
        assertEquals(DEVICE_COUNT ,result.size());
        validateShiftedUdi(result);

        result.clear();

        ReserveDeviceUtil.reserveDeviceMessages(result, buildConnections(connectionSize, ElementType.BATTERY_NOD), buildShiftedIndexes(DEVICE_COUNT, SHIFTED_IDX), PERIOD, ElementType.BATTERY_NOD);
        assertEquals(DEVICE_COUNT ,result.size());
        validateShiftedUdi(result);
    }

    private void deleteFile()
    {
        if(reserved.delete())
            System.out.println("file deleted");
        else
            System.out.println("file not deleted");
    }

    private void validateDevice(ReservedDevice device, int idx, long sequenceNumber)
    {
        assertNotNull(device);

        assertEquals((int)sequenceNumber+ idx, device.getStartIndex());
        assertEquals(PERIOD, device.getPeriod());
    }

    private Map<String, ReservedDevice> buildDevices(long sequenceNumber)
    {
        return IntStream.rangeClosed(1, DEVICE_COUNT)
                .boxed()
                .collect(Collectors.toMap(idx -> DEVICE_PREFIX+idx , idx -> new ReservedDevice( (int)sequenceNumber + idx, PERIOD.toString(PERIOD_STRING_FORMAT)) ));
    }

    private Map<String, ReservedDevice> buildDevices(long sequenceNumber, int offset)
    {
        return IntStream.rangeClosed(1, DEVICE_COUNT)
                .boxed()
                .collect(Collectors.toMap(idx -> DEVICE_PREFIX + (idx+offset) , idx -> new ReservedDevice( (int)sequenceNumber + idx + offset, PERIOD.toString(PERIOD_STRING_FORMAT)) ));
    }

    private List<ConnectionPortfolioDto> buildConnections(int size, String type)
    {
        return IntStream.range(0, size)
                        .mapToObj(idx ->
                        {
                            ConnectionPortfolioDto connection =  new ConnectionPortfolioDto(EANUtil.EAN_PREFIX + idx);
                            connection.getUdis().add(buildBatteryUdi(idx, type));
                            return connection;
                        })
                        .collect(Collectors.toList());
    }

    private UdiPortfolioDto buildBatteryUdi(int idx, String type)
    {
        UdiPortfolioDto battery =  new UdiPortfolioDto(DEVICE_PREFIX + idx, 96, type);

        PowerContainerDto power = new PowerContainerDto(PERIOD, SHIFTED_IDX);
        switch (type)
        {
            case ElementType.BATTERY_ZIH:
                power.getForecast().setPotentialFlexConsumption(BigInteger.ONE);
                break;

            case ElementType.BATTERY_NOD:
                power.getForecast().setPotentialFlexProduction(BigInteger.ONE);
                break;
        }
        battery.getUdiPowerPerDTU().put(SHIFTED_IDX, power);

        return battery;
    }

    private List<Integer> buildShiftedIndexes(int size, Integer shiftedIndex)
    {
        return IntStream.range(0, size)
                        .mapToObj(idx -> shiftedIndex)
                        .collect(Collectors.toList());
    }

    private void validateShiftedUdi(Map<String, ReservedDevice> deviceMap)
    {
        for(int idx = 0; idx < DEVICE_COUNT; idx++)
        {
            ReservedDevice resultDevice = deviceMap.get(DEVICE_PREFIX + idx);

            assertNotNull(resultDevice);
            assertEquals(PERIOD, resultDevice.getPeriod());
            assertEquals(60, resultDevice.getStartIndex());
        }
    }
}