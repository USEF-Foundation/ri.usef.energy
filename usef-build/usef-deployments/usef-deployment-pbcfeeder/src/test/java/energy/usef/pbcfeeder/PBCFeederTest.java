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

package energy.usef.pbcfeeder;

import static org.junit.Assert.assertEquals;

import energy.usef.pbcfeeder.dto.PbcStubDataDto;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link PbcFeeder} class.
 */
public class PBCFeederTest {

    private PbcFeeder pbcFeeder;

    private static final String TEST_PBC_FEEDER_EXCEL_SHEET = "stubinputdata.xls";

    @Before
    public void init() throws URISyntaxException {
        pbcFeeder = new PbcFeeder();
        pbcFeeder.readFile(
                Paths.get(Thread.currentThread().getContextClassLoader().getResource(TEST_PBC_FEEDER_EXCEL_SHEET).toURI()));
    }

    @Test
    public void testGetStubRowInputListWithStartPtuOne() {
        LocalDate date = new LocalDate("2015-04-22");
        int startPtu = 1;
        int amount = 96;
        List<PbcStubDataDto> stubRowList = pbcFeeder.getStubRowInputList(date, startPtu, amount);
        assertEquals(stubRowList.size(), 96);
    }

    @Test
    public void testGetStubRowInputListWithMultipleDays() {
        LocalDate date = new LocalDate("2015-04-22");
        int startPtu = 1;
        int amount = 200;
        List<PbcStubDataDto> stubRowList = pbcFeeder.getStubRowInputList(date, startPtu, amount);
        assertEquals(stubRowList.size(), 200);
        assertEquals(new LocalDate("2015-04-22").toDateMidnight().toDate(),
                stubRowList.get(0).getPtuContainer().getPtuDate());
        assertEquals(new LocalDate("2015-04-23").toDateMidnight().toDate(),
                stubRowList.get(96).getPtuContainer().getPtuDate());
        assertEquals(new LocalDate("2015-04-24").toDateMidnight().toDate(),
                stubRowList.get(196).getPtuContainer().getPtuDate());
        assertEquals(1, stubRowList.get(96).getPtuContainer().getPtuIndex());
        assertEquals(1, stubRowList.get(0).getPtuContainer().getPtuIndex());
    }

    @Test
    public void testGetStubRowInputListWithStartPtu197() {
        LocalDate date = new LocalDate("2015-04-22");
        int startPtu = 197;
        int amount = 1;
        List<PbcStubDataDto> stubRowList = pbcFeeder.getStubRowInputList(date, startPtu, amount);
        assertEquals(new LocalDate("2015-04-24").toDateMidnight().toDate(),
                stubRowList.get(0).getPtuContainer().getPtuDate());
    }

    @Test
    public void testGetStubRowInputListWithStartAtPtu96() {
        LocalDate date = new LocalDate("2015-04-21");
        int startPtu = 96;
        int amount = 3;
        List<PbcStubDataDto> stubRowList = pbcFeeder.getStubRowInputList(date, startPtu, amount);
        assertEquals(stubRowList.size(), 3);
        assertEquals(stubRowList.get(0).getPtuContainer().getPtuDate(),
                new LocalDate("2015-04-21").toDateMidnight().toDate());
        assertEquals(stubRowList.get(1).getPtuContainer().getPtuDate(),
                new LocalDate("2015-04-22").toDateMidnight().toDate());
    }

    /**
     * CHANGES TO THE STUBINPUTDATA.XLS MIGHT BREAK THIS TEST.
     *
     * @throws Exception
     */
    @Test
    public void testGetUncontrolledLoadForConnection() throws Exception {
        List<Double> cpOne = pbcFeeder.getUncontrolledLoadForCongestionPoint(1);
        assertEquals(236.94000245, cpOne.get(0), 0.5);
    }

    /**
     * CHANGES TO THE STUBINPUTDATA.XLS MIGHT BREAK THIS TEST.
     *
     * @throws Exception
     */
    @Test
    public void testGetUncontrolledLoadForConnectionWithInvalidArgumentReturnsAvgColumn() {
        List<Double> cpAvg = pbcFeeder.getUncontrolledLoadForCongestionPoint(99);
        assertEquals(267.95043946, cpAvg.get(0), 0.5);
    }

    /**
     * CHANGES TO THE STUBINPUTDATA.XLS MIGHT BREAK THIS TEST.
     *
     * @throws Exception
     */
    @Test
    public void testGetPvForecast() throws Exception {
        List<Double> pvForecast = pbcFeeder.getPvForecast();
        assertEquals(19.64430284, pvForecast.get(17), 0.5);
    }

    /**
     * CHANGES TO THE STUBINPUTDATA.XLS MIGHT BREAK THIS TEST.
     *
     * @throws Exception
     */
    @Test
    public void testGetPvActual() throws Exception {
        List<Double> pvActual = pbcFeeder.getPvActual();
        assertEquals(16.57735456, pvActual.get(18), 0.5);
    }

    /**
     * CHANGES TO THE STUBINPUTDATA.XLS MIGHT BREAK THIS TEST.
     *
     * @throws Exception
     */
    @Test
    public void testGetApx() throws Exception {
        List<Double> apx = pbcFeeder.getApx();
        assertEquals(61.19077659, apx.get(0), 0.5);
    }

    /**
     * CHANGES TO THE STUBINPUTDATA.XLS MIGHT BREAK THIS TEST.
     *
     * @throws Exception
     */
    @Test
    public void testGetPtu() throws Exception {
        List<Double> ptuStartTime = pbcFeeder.getPtuStartTime();
        assertEquals(0, ptuStartTime.get(0), 20);
    }

    @Test
    public void testGetCongestionPointPowerLimitsForCongestionPoint1() {
        List<BigDecimal> powerLimits = pbcFeeder.getCongestionPointPowerLimits(1);
        Assert.assertEquals(2, powerLimits.size());
        Assert.assertEquals(BigDecimal.valueOf(0), powerLimits.get(0));
        Assert.assertEquals(BigDecimal.valueOf(27500), powerLimits.get(1));
    }

    @Test
    public void testGetCongestionPointPowerLimitsForCongestionPoint2() {
        List<BigDecimal> powerLimits = pbcFeeder.getCongestionPointPowerLimits(2);
        Assert.assertEquals(2, powerLimits.size());
        Assert.assertEquals(BigDecimal.valueOf(-22000), powerLimits.get(0));
        Assert.assertEquals(BigDecimal.valueOf(55000), powerLimits.get(1));
    }

    @Test
    public void testGetCongestionPointPowerLimitsForCongestionPoint3() {
        List<BigDecimal> powerLimits = pbcFeeder.getCongestionPointPowerLimits(3);
        Assert.assertEquals(2, powerLimits.size());
        Assert.assertEquals(BigDecimal.valueOf(0), powerLimits.get(0));
        Assert.assertEquals(BigDecimal.valueOf(250000), powerLimits.get(1));
    }

    @Test
    public void testGetCongestionPointPowerLimitsForUnexistingCongestionPoint() {
        List<BigDecimal> powerLimits = pbcFeeder.getCongestionPointPowerLimits(4);
        Assert.assertEquals(0, powerLimits.size());
    }

}
