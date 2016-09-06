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

package energy.usef.mdc.pbcfeederimpl;

import energy.usef.core.util.DateTimeUtil;
import energy.usef.mdc.dto.ConnectionMeterDataDto;
import energy.usef.pbcfeeder.PbcFeederClient;
import energy.usef.pbcfeeder.dto.PbcPtuContainerDto;
import energy.usef.pbcfeeder.dto.PbcStubDataDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link PbcFeederService} class.
 */
@RunWith(PowerMockRunner.class)
public class PbcFeederServiceTest {

    @Mock
    private PbcFeederClient pbcFeederClient;

    private PbcFeederService pbcFeederService;

    @Before
    public void setUp() throws Exception {
        pbcFeederService = new PbcFeederService();
        Whitebox.setInternalState(pbcFeederService, pbcFeederClient);
        stubPbcFeederClient();
    }

    @Test
    public void testFetchUncontrolledLoad() {
        List<ConnectionMeterDataDto> connectionMeterDataDtos = pbcFeederService.fetchUncontrolledLoad(DateTimeUtil.getCurrentDate(),
                1, 12, Arrays.asList("ean.00000000001", "ean.000000000002"));
        Assert.assertEquals(2, connectionMeterDataDtos.size());
        connectionMeterDataDtos.stream()
                .forEach(connectionMeterDataDto -> Assert.assertEquals(12, connectionMeterDataDto.getPtuMeterDataDtos().size()));
    }

    private void stubPbcFeederClient() {
        PowerMockito.when(pbcFeederClient.getPbcStubDataList(DateTimeUtil.getCurrentDate(), 1, 12)).then(invocation -> {
            List<PbcStubDataDto> stubData = new ArrayList<>();
            Integer start = (Integer) invocation.getArguments()[1];
            IntStream.range(start, start + (Integer) invocation.getArguments()[2]).mapToObj(ptuIndex -> {
                PbcStubDataDto dto = new PbcStubDataDto();
                dto.setCongestionPointAvg(10D);
                dto.setPtuContainer(
                        new PbcPtuContainerDto(((LocalDate) invocation.getArguments()[0]).toDateMidnight().toDate(), ptuIndex));
                return dto;
            }).forEach(stubData::add);
            return stubData;
        });
    }
}
