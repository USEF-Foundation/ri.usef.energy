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

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

/**
 * Data Transfer Object for a FlexRequest.
 */
public class FlexRequestDto {

    private String connectionGroupEntityAddress;
    private LocalDate period;
    private String participantDomain;
    private USEFRoleDto participantRole;
    private Long sequenceNumber;
    private Long prognosisSequenceNumber;
    private LocalDateTime expirationDateTime;
    private List<PtuFlexRequestDto> ptus;

    public String getConnectionGroupEntityAddress() {
        return connectionGroupEntityAddress;
    }

    public void setConnectionGroupEntityAddress(String connectionGroupEntityAddress) {
        this.connectionGroupEntityAddress = connectionGroupEntityAddress;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public void setPeriod(LocalDate period) {
        this.period = period;
    }

    public String getParticipantDomain() {
        return participantDomain;
    }

    public void setParticipantDomain(String participantDomain) {
        this.participantDomain = participantDomain;
    }

    public USEFRoleDto getParticipantRole() {
        return participantRole;
    }

    public void setParticipantRole(USEFRoleDto participantRole) {
        this.participantRole = participantRole;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Long getPrognosisSequenceNumber() {
        return prognosisSequenceNumber;
    }

    public void setPrognosisSequenceNumber(Long prognosisSequenceNumber) {
        this.prognosisSequenceNumber = prognosisSequenceNumber;
    }

    public LocalDateTime getExpirationDateTime() {
        return expirationDateTime;
    }

    public void setExpirationDateTime(LocalDateTime expirationDateTime) {
        this.expirationDateTime = expirationDateTime;
    }

    public List<PtuFlexRequestDto> getPtus() {
        if (ptus == null) {
            ptus = new ArrayList<>();
        }
        return ptus;
    }

    public void setPtus(List<PtuFlexRequestDto> ptus) {
        this.ptus = ptus;
    }

    @Override
    public String toString() {
        return "FlexRequestDto" + "[" +
                "expirationDateTime=" + expirationDateTime +
                ", connectionGroupEntityAddress='" + connectionGroupEntityAddress + "'" +
                ", period=" + period +
                ", participantDomain='" + participantDomain + "'" +
                ", participantRole=" + participantRole +
                ", sequenceNumber=" + sequenceNumber +
                ", prognosisSequenceNumber=" + prognosisSequenceNumber +
                "]";
    }
}
