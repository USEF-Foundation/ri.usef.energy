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

package energy.usef.core.workflow.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO class to carry information coming from the MDC participant.
 */
public class MeterDataSetDto {

    private List<MeterDataDto> meterDataDtos;
    private String entityAddress;

    /**
     * Constructor with the USEF identifier of the {@link MeterDataSetDto}.
     *
     * @param entityAddress {@link String} USEF identifier.
     */
    public MeterDataSetDto(String entityAddress) {
        this.entityAddress = entityAddress;
    }

    public List<MeterDataDto> getMeterDataDtos() {
        if (meterDataDtos == null) {
            meterDataDtos = new ArrayList<>();
        }
        return meterDataDtos;
    }

    public String getEntityAddress() {
        return entityAddress;
    }

    public void setEntityAddress(String entityAddress) {
        this.entityAddress = entityAddress;
    }
}
