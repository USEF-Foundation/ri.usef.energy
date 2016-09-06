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

import static org.junit.Assert.assertNull;

import energy.usef.core.model.AgrConnectionGroup;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.dso.model.NonAggregatorForecast;
import energy.usef.dso.workflow.dto.NonAggregatorForecastDto;
import energy.usef.dso.workflow.dto.PtuNonAggregatorForecastDto;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for the class {@link NonAggregatorForecastDtoTransformer}.
 */
@RunWith(PowerMockRunner.class)
public class NonAggregatorForecastDtoTransformerTest {

    @Test
    public void testTransform() throws Exception {
        NonAggregatorForecast nonAggregatorForecast = null;
        assertNull(NonAggregatorForecastDtoTransformer.transform(nonAggregatorForecast));

        nonAggregatorForecast = new NonAggregatorForecast();
        nonAggregatorForecast.setPtuContainer(new PtuContainer(DateTimeUtil.getCurrentDate(), 1));
        nonAggregatorForecast.setSequence(2l);
        nonAggregatorForecast.setMaxLoad(100l);
        nonAggregatorForecast.setPower(200l);

        PtuNonAggregatorForecastDto dto = NonAggregatorForecastDtoTransformer.transform(nonAggregatorForecast);

        Assert.assertEquals(nonAggregatorForecast.getPtuContainer().getPtuIndex(), dto.getPtuIndex());
        Assert.assertEquals(nonAggregatorForecast.getPower(), dto.getPower());
        Assert.assertEquals(nonAggregatorForecast.getMaxLoad(), dto.getMaxLoad());
    }

    @Test
    public void testTransformList() throws Exception {
        List<NonAggregatorForecast> nonAggregatorForecastList = null;
        assertNull(NonAggregatorForecastDtoTransformer.transform(nonAggregatorForecastList));

        NonAggregatorForecast forecast1 = buildNonAggregatorForecast(1);
        NonAggregatorForecast forecast2 = buildNonAggregatorForecast(2);

        nonAggregatorForecastList = Arrays.asList(forecast1, forecast2);

        NonAggregatorForecastDto dto = NonAggregatorForecastDtoTransformer.transform(nonAggregatorForecastList);
        Assert.assertEquals("agr1.usef-example.com", dto.getEntityAddress());
        Assert.assertEquals(DateTimeUtil.getCurrentDate(), dto.getPtuDate());
        Assert.assertEquals(2, dto.getPtus().size());
    }

    private NonAggregatorForecast buildNonAggregatorForecast(int ptuIndex) {
        NonAggregatorForecast forecast = new NonAggregatorForecast();
        forecast.setSequence((long) ptuIndex);
        forecast.setPtuContainer(new PtuContainer(DateTimeUtil.getCurrentDate(), ptuIndex));
        forecast.setPower(100l * ptuIndex);
        forecast.setMaxLoad(200l * ptuIndex);
        forecast.setConnectionGroup(new AgrConnectionGroup("agr1.usef-example.com"));

        return forecast;
    }
}
