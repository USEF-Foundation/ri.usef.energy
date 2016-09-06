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

package energy.usef.dso.pbcfeederimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import energy.usef.pbcfeeder.PbcFeederClient;
import energy.usef.pbcfeeder.dto.PbcPowerLimitsDto;
import energy.usef.pbcfeeder.dto.PbcPtuContainerDto;
import energy.usef.pbcfeeder.dto.PbcStubDataDto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link PbcFeederService} class of the DSO participant.
 */
@RunWith(PowerMockRunner.class) public class PbcFeederServiceTest {

    private PbcFeederService pbcFeederService;
    @Mock
    private PbcFeederClient pbcFeederClient;

    @Before
    public void setUp() {
        pbcFeederService = new PbcFeederService();
        Whitebox.setInternalState(pbcFeederService, pbcFeederClient);
    }

    @Test
    public void testGetUncontrolledLoadIsSuccessful() {
        // local variables
        String congestionPointEntityAddress = "ean.123456789012345678";
        LocalDate period = new LocalDate("2015-02-02");
        int startIndex = 1;
        int amountOfPtus = 48;
        // Stubbing of the rest service
        stubRestService(period);
        // actual call to the PBCFeederUtil
        double uncontrolledLoad = pbcFeederService
                .getUncontrolledLoad(congestionPointEntityAddress, period, startIndex, amountOfPtus);

        // validations
        double delta = 0.001d;
        double expectedUncontrolledLoad = 4800d; // 48 * 100
        Assert.assertNotNull(uncontrolledLoad);
        Assert.assertEquals(expectedUncontrolledLoad, uncontrolledLoad, delta);
    }

    @Test
    public void testGetCongestionPointPowerLimits() {
        // stubbing
        PowerMockito.when(pbcFeederClient.getCongestionPointPowerLimits(Matchers.anyInt()))
                .thenReturn(Arrays.asList(BigDecimal.TEN, BigDecimal.valueOf(20L)));
        // invocation
        PbcPowerLimitsDto powerLimits = pbcFeederService.getCongestionPointPowerLimits("ean.123456789012345678");
        // assertions
        Assert.assertNotNull(powerLimits);
        Assert.assertEquals(new BigDecimal("10"), powerLimits.getLowerLimit());
        Assert.assertEquals(new BigDecimal("20"), powerLimits.getUpperLimit());
    }

    private void stubRestService(LocalDate period) {
        Mockito.when(pbcFeederClient.getPbcStubDataList(Matchers.eq(period), Matchers.eq(1), Matchers.any(Integer.class)))
                .then(invocation -> IntStream.rangeClosed(1, (Integer) invocation.getArguments()[2]).mapToObj(index -> {
                    PbcStubDataDto pbcStubDataDto = new PbcStubDataDto();
                    PbcPtuContainerDto ptuContainerDto = new PbcPtuContainerDto(period.toDateMidnight().toDate(), index);
                    pbcStubDataDto.setCongestionPointOne(50.00);
                    pbcStubDataDto.setCongestionPointTwo(100.00);
                    pbcStubDataDto.setPtuContainer(ptuContainerDto);
                    pbcStubDataDto.setIndex(index);
                    return pbcStubDataDto;
                }).collect(Collectors.toList()));
    }

    @Test
    public void testRetrieveApxPrices() throws Exception {
        List<PbcStubDataDto> values = IntStream.range(1, 6).mapToObj(BigDecimal::valueOf).map(value -> {
            PbcStubDataDto dto = new PbcStubDataDto();
            dto.setApx(100d * value.doubleValue());
            dto.setPtuContainer(new PbcPtuContainerDto());
            dto.getPtuContainer().setPtuIndex(value.intValue());
            return dto;
        }).collect(Collectors.toList());

        Mockito.when(pbcFeederClient.getPbcStubDataList(Mockito.any(), Mockito.eq(1), Mockito.eq(5))).thenReturn(values);

        Map<Integer, BigDecimal> results = pbcFeederService.retrieveApxPrices(new LocalDate(), 1, 5);
        assertNotNull(results);
        assertEquals(5, results.size());

        assertEquals(BigDecimal.valueOf(100.0), results.get(1));
        assertEquals(BigDecimal.valueOf(200.0), results.get(2));
        assertEquals(BigDecimal.valueOf(300.0), results.get(3));
        assertEquals(BigDecimal.valueOf(400.0), results.get(4));
        assertEquals(BigDecimal.valueOf(500.0), results.get(5));
    }
}
