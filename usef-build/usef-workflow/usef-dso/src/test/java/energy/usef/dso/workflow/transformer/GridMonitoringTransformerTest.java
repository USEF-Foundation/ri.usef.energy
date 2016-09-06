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

package energy.usef.dso.workflow.transformer;

import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.PtuContainer;
import energy.usef.dso.model.PtuGridMonitor;
import energy.usef.dso.workflow.dto.GridMonitoringDto;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link GridMonitoringTransformer} class.
 */
public class GridMonitoringTransformerTest {

    private static final String CONGESTION_POINT = "ean.111111111111";
    private static final LocalDate START_DATE = new LocalDate(2105, 1, 1);

    @Test
    public void testTransform() throws Exception {
        GridMonitoringDto gridMonitoringDto = GridMonitoringTransformer.transform(buildGridMonitoringData());
        Assert.assertNotNull(gridMonitoringDto);
        Assert.assertEquals(CONGESTION_POINT, gridMonitoringDto.getCongestionPointEntityAddress());
        Assert.assertEquals(START_DATE, gridMonitoringDto.getPeriod());
        IntStream.rangeClosed(1, 96).forEach(index -> {
            Assert.assertEquals(BigInteger.valueOf(1000 + index),
                    gridMonitoringDto.getPtuGridMonitoringDtos().get(index - 1).getActualPower());
            Assert.assertEquals(index,
                    gridMonitoringDto.getPtuGridMonitoringDtos().get(index - 1).getPtuIndex().intValue());
        });
    }

    @Test
    public void testTransformWithNull() throws Exception {
        Assert.assertNull(GridMonitoringTransformer.transform(null));
    }

    private List<PtuGridMonitor> buildGridMonitoringData() {
        final ConnectionGroup connectionGroup = new CongestionPointConnectionGroup(CONGESTION_POINT);
        return IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuGridMonitor ptuGridMonitor = new PtuGridMonitor();
            ptuGridMonitor.setPtuContainer(new PtuContainer(START_DATE, index));
            ptuGridMonitor.setConnectionGroup(connectionGroup);
            ptuGridMonitor.setActualPower(1000L + index);
            return ptuGridMonitor;
        }).collect(Collectors.toList());
    }

}
