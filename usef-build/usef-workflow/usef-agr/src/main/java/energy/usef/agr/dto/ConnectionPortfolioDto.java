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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import energy.usef.core.model.ConnectionGroup;

/**
 * A portfolio is per {@link ConnectionGroup}. This object contains the portfolio filled with a
 */
public class ConnectionPortfolioDto {

    private String connectionEntityAddress;

    private Map<Integer, PowerContainerDto> connectionPowerPerPTU;

    private List<UdiPortfolioDto> udis;

    /**
     * Constructs a {@link ConnectionPortfolioDto} for the specified connection entity address.
     *
     * @param connectionEntityAddress {@link String} a connection entity address.
     */
    public ConnectionPortfolioDto(String connectionEntityAddress) {
        this.connectionEntityAddress = connectionEntityAddress;
    }

    public String getConnectionEntityAddress() {
        return connectionEntityAddress;
    }

    public Map<Integer, PowerContainerDto> getConnectionPowerPerPTU() {
        if (connectionPowerPerPTU == null) {
            connectionPowerPerPTU = new HashMap<>();
        }
        return connectionPowerPerPTU;
    }

    public List<UdiPortfolioDto> getUdis() {
        if (udis == null) {
            udis = new ArrayList<>();
        }
        return udis;
    }

    @Override
    public int hashCode() {
        return connectionEntityAddress.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof ConnectionPortfolioDto)) {
            return false;
        }
        if (this.connectionEntityAddress == null) {
            return false;
        }
        return this.connectionEntityAddress.equals(((ConnectionPortfolioDto) other).connectionEntityAddress);
    }

    @Override
    public String toString() {
        return "ConnectionPortfolioDTO" + "[" +
                "connectionEntityAddress='" + connectionEntityAddress + "'" +
                ", connectionPowerPerPTU=" + connectionPowerPerPTU +
                "]";
    }
}
