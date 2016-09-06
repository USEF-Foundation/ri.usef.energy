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

/**
 * DTO for the D-Prognosis Input Data.
 */
public class PrognosisDto {

    private String connectionGroupEntityAddress;
    private LocalDate period;
    private String participantDomain;
    private USEFRoleDto participantRole;
    private Long sequenceNumber;
    private PrognosisTypeDto type;
    private List<PtuPrognosisDto> ptus;
    private boolean substitute = false;

    public PrognosisDto() {
        // default constructor. Empty.
    }

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

    public PrognosisTypeDto getType() {
        return type;
    }

    public void setType(PrognosisTypeDto type) {
        this.type = type;
    }

    public List<PtuPrognosisDto> getPtus() {
        if (ptus == null) {
            ptus = new ArrayList<>();
        }
        return ptus;
    }

    public void setPtus(List<PtuPrognosisDto> ptus) {
        this.ptus = ptus;
    }

    public boolean isSubstitute() {
        return substitute;
    }

    public void setSubstitute(boolean substitute) {
        this.substitute = substitute;
    }

}
