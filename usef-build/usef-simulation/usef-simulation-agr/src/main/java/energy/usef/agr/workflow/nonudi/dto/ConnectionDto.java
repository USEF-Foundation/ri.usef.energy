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

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Data Transfer Object (DTO) that represents a Connection (CONN). Connections are always associated with one BRP and optionally
 * a Congestion Point. When the connection is not associated with a Congestion Point, the value of the Congestion Point identifier
 * is null.
 */
public class ConnectionDto {

    @JsonProperty("conn_id")
    private String connectionId;

    @JsonProperty("brp_id")
    private String brpId;

    @JsonProperty("cp_id")
    private String congestionPointId;

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getBrpId() {
        return brpId;
    }

    public void setBrpId(String brpId) {
        this.brpId = brpId;
    }

    public String getCongestionPointId() {
        return congestionPointId;
    }

    public void setCongestionPointId(String congestionPointId) {
        this.congestionPointId = congestionPointId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConnectionDto)) {
            return false;
        }

        final ConnectionDto connectionDto = (ConnectionDto) obj;

        if ((this.brpId == null) ? (connectionDto.getBrpId() != null) : !this.brpId.equals(connectionDto.getBrpId())) {
            return false;
        }

        if ((this.congestionPointId == null) ?
                (connectionDto.getCongestionPointId() != null) :
                !this.congestionPointId.equals(connectionDto.getCongestionPointId())) {
            return false;
        }

        return !((this.connectionId == null) ?
                (connectionDto.getConnectionId() != null) :
                !this.connectionId.equals(connectionDto.getConnectionId()));

    }

    @Override
    public int hashCode() {
        int result = connectionId.hashCode();
        result = 31 * result + congestionPointId.hashCode();
        result = 31 * result + brpId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ConnectionDto[" +
                "connectionId=" + connectionId +
                ", brpId=" + brpId +
                ", congestionPointId=" + congestionPointId +
                "]";
    }
}
