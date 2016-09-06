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

package energy.usef.agr.workflow.nonudi.dto;

import java.util.Objects;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Data Transfer Object (DTO) that represents the Balance Responsible Party (BRP).
 */
public class BalanceResponsiblePartyDto {

    @JsonProperty("brp_id")
    private String brpId;

    private BalanceResponsiblePartyDto() {
        // private constructor
    }

    /**
     * Constructor for creating BalanceResponsiblePartyDto object with specific BRP id.
     *
     * @param brpId
     */
    public BalanceResponsiblePartyDto(String brpId) {
        this.brpId = brpId;
    }

    public String getBrpId() {
        return brpId;
    }

    public void setBrpId(String brpId) {
        this.brpId = brpId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BalanceResponsiblePartyDto)) {
            return false;
        }

        return this.brpId.equals(((BalanceResponsiblePartyDto) obj).getBrpId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(brpId);
    }

    @Override
    public String toString() {
        return "BalanceResponsiblePartyDto[" +
                "brpId=" + brpId +
                "]";
    }
}
