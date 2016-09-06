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

package energy.usef.brp.pbcfeederimpl;

import energy.usef.pbcfeeder.PbcFeederClient;
import energy.usef.pbcfeeder.dto.PbcPtuContainerDto;
import energy.usef.pbcfeeder.dto.PbcStubDataDto;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PowerMockRunner.class)
public class PbcFeederServiceTest {

    private PbcFeederService pbcFeederService;

    @Mock
    private PbcFeederClient pbcFeederClient;

    @Before
    public void init() {
        pbcFeederService = new PbcFeederService();
        Whitebox.setInternalState(pbcFeederService, pbcFeederClient);
    }

    @Test
    public void testRetrieveApxPrices() throws Exception {
        List<PbcStubDataDto> values = IntStream.range(1, 6).mapToObj(BigDecimal::valueOf)
                .map(value -> {
                    PbcStubDataDto dto = new PbcStubDataDto();
                    dto.setApx(100d * value.doubleValue());
                    dto.setPtuContainer(new PbcPtuContainerDto());
                    dto.getPtuContainer().setPtuIndex(value.intValue());
                    return dto;
                }).collect(Collectors.toList());

        Mockito.when(pbcFeederClient.getPbcStubDataList(Mockito.any(), Mockito.eq(1), Mockito.eq(5)))
                .thenReturn(values);

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
