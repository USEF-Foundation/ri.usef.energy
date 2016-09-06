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

import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.workflow.dto.USEFRoleDto;

import java.util.HashMap;
import java.util.Map;

/**
 * A portfolio is per {@link ConnectionGroup}. This object contains the portfolio filled with a
 */
public class ConnectionGroupPortfolioDto {

    private USEFRoleDto usefRole;
    private String usefIdentifier;
    private Map<Integer, PowerContainerDto> connectionGroupPowerPerPTU;

    /**
     * Constructs a {@link ConnectionGroupPortfolioDto} for the specified USEF identifier and USEF role.
     *
     * @param usefIdentifier {@link String} a USEF identifier.
     * @param usefRole {@link USEFRole} a USEF role.
     */
    public ConnectionGroupPortfolioDto(String usefIdentifier, USEFRoleDto usefRole) {
        this.usefIdentifier = usefIdentifier;
        this.usefRole = usefRole;
    }

    public String getUsefIdentifier() {
        return usefIdentifier;
    }

    public USEFRoleDto getUsefRole() {
        return usefRole;
    }

    public Map<Integer, PowerContainerDto> getConnectionGroupPowerPerPTU() {
        if (connectionGroupPowerPerPTU == null) {
            connectionGroupPowerPerPTU = new HashMap<>();
        }
        return connectionGroupPowerPerPTU;
    }

    @Override
    public String toString() {
        return "ConnectionGroupPortfolioDTO" + "[" +
                "usefRole=" + usefRole +
                ", usefIdentifier='" + usefIdentifier + "'" +
                "]";
    }
}

