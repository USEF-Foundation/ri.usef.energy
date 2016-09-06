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
 * Data Transfer Object (DTO) that represents a Congestion Point (CP).
 */
public class CongestionPointDto {

    @JsonProperty("cp_id")
    private String cpId;

    public CongestionPointDto() {
        //default constructor
    }

    /**
     * Instantiate a CongestionPointDto withe the specified cpId.
     *
     * @param cpId
     */
    public CongestionPointDto(String cpId) {
        this.cpId = cpId;
    }

    public String getCpId() {
        return cpId;
    }

    public void setCpId(String cpId) {
        this.cpId = cpId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CongestionPointDto)) {
            return false;
        }

        return this.cpId.equals(((CongestionPointDto) obj).getCpId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cpId);
    }

    @Override
    public String toString() {
        return "CongestionPointDto[" +
                "cpId=" + cpId +
                "]";
    }
}
