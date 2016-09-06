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

import energy.usef.dso.model.NonAggregatorForecast;
import energy.usef.dso.workflow.dto.NonAggregatorForecastDto;
import energy.usef.dso.workflow.dto.PtuNonAggregatorForecastDto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO Transformer related to the {@link NonAggregatorForecastDto}.
 */
public class NonAggregatorForecastDtoTransformer {
    private NonAggregatorForecastDtoTransformer() {
        // private constructor
    }

    /**
     * Transform a List of {@link NonAggregatorForecast} to a
     * {@link NonAggregatorForecastDto}.
     *
     * @param nonAggregatorForecastList
     * @return
     */
    public static NonAggregatorForecastDto transform(List<NonAggregatorForecast> nonAggregatorForecastList) {
        if (nonAggregatorForecastList == null || nonAggregatorForecastList.isEmpty()) {
            return null;
        }
        NonAggregatorForecast firstNonAggregatorForecast = nonAggregatorForecastList.get(0);
        NonAggregatorForecastDto nonAggregatorForecastDto = new NonAggregatorForecastDto();
        nonAggregatorForecastDto.setEntityAddress(firstNonAggregatorForecast.getConnectionGroup().getUsefIdentifier());
        nonAggregatorForecastDto.setPtuDate(firstNonAggregatorForecast.getPtuContainer().getPtuDate());

        nonAggregatorForecastDto.setPtus(nonAggregatorForecastList.stream()
                .map(NonAggregatorForecastDtoTransformer::transform)
                .collect(Collectors.toList()));

        return nonAggregatorForecastDto;
    }

    /**
     * Transforms a {@link NonAggregatorForecast} to a
     * {@link NonAggregatorForecastDto}.
     *
     * @param nonAggregatorForecast
     * @return
     */
    public static PtuNonAggregatorForecastDto transform(NonAggregatorForecast nonAggregatorForecast) {
        if (nonAggregatorForecast == null) {
            return null;
        }
        PtuNonAggregatorForecastDto ptuNonAggregatorForecastDto = new PtuNonAggregatorForecastDto();
        ptuNonAggregatorForecastDto.setPower(nonAggregatorForecast.getPower());
        ptuNonAggregatorForecastDto.setPtuIndex(nonAggregatorForecast.getPtuContainer().getPtuIndex());
        ptuNonAggregatorForecastDto.setMaxLoad(nonAggregatorForecast.getMaxLoad());
        return ptuNonAggregatorForecastDto;
    }
}
