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

package energy.usef.agr.dto;

import java.util.HashMap;
import java.util.Map;

import energy.usef.core.model.ConnectionGroup;

/**
 * A portfolio is per {@link ConnectionGroup}. This object contains the portfolio filled with a
 */
public class UdiPortfolioDto {

    private String endpoint;
    private Integer dtuSize;
    private String profile;
    private Map<Integer, PowerContainerDto> udiPowerPerDTU;

    /**
     * Constructs a {@link ConnectionGroupPortfolioDto} for the specified endpoint, DTU size and UDI profile type.
     *
     * @param endpoint {@link String} an endpoint.
     * @param dtuSize {@link Integer} a DTU size.
     * @param profile {@link String} UDI profile type.
     */
    public UdiPortfolioDto(String endpoint, Integer dtuSize, String profile) {
        this.endpoint = endpoint;
        this.dtuSize = dtuSize;
        this.profile = profile;
    }

    public Integer getDtuSize() {
        return dtuSize;
    }

    public String getProfile() {
        return profile;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Map<Integer, PowerContainerDto> getUdiPowerPerDTU() {
        if (udiPowerPerDTU == null) {
            udiPowerPerDTU = new HashMap<>();
        }
        return udiPowerPerDTU;
    }

    @Override public String toString() {
        return "UDIPortfolioDTO" + "[" +
                "endpoint='" + endpoint + "'" +
                ", dtuSize=" + dtuSize +
                ", profile='" + profile + "'" +
                "]";
    }
}
