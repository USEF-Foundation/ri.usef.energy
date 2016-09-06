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
 * Data Transfer Object for a FlexOrder.
 */
public class FlexOrderDto {

    private String connectionGroupEntityAddress;
    private LocalDate period;
    private Long flexOfferSequenceNumber;
    private Long sequenceNumber;
    private LocalDateTime expirationDateTime;
    private AcknowledgementStatusDto acknowledgementStatus;
    private String participantDomain;
    private List<PtuFlexOrderDto> ptus;

    /**
     * Default constructor.
     */
    public FlexOrderDto() {
        // empty constructor, do nothing.
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

    public Long getFlexOfferSequenceNumber() {
        return flexOfferSequenceNumber;
    }

    public void setFlexOfferSequenceNumber(Long flexOfferSequenceNumber) {
        this.flexOfferSequenceNumber = flexOfferSequenceNumber;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public LocalDateTime getExpirationDateTime() {
        return expirationDateTime;
    }

    public void setExpirationDateTime(LocalDateTime expirationDateTime) {
        this.expirationDateTime = expirationDateTime;
    }

    public AcknowledgementStatusDto getAcknowledgementStatus() {
        return acknowledgementStatus;
    }

    public void setAcknowledgementStatus(AcknowledgementStatusDto acknowledgementStatus) {
        this.acknowledgementStatus = acknowledgementStatus;
    }

    public String getParticipantDomain() {
        return participantDomain;
    }

    public void setParticipantDomain(String participantDomain) {
        this.participantDomain = participantDomain;
    }

    public List<PtuFlexOrderDto> getPtus() {
        if (ptus == null) {
            ptus = new ArrayList<>();
        }
        return ptus;
    }

    @Override
    public String toString() {
        return "FlexOrderDto" + "[" +
                "connectionGroupEntityAddress='" + connectionGroupEntityAddress + "'" +
                ", period=" + period +
                ", flexOfferSequenceNumber=" + flexOfferSequenceNumber +
                ", sequenceNumber=" + sequenceNumber +
                ", expirationDateTime=" + expirationDateTime +
                ", acknowledgementStatus=" + acknowledgementStatus +
                ", participantDomain='" + participantDomain + "'" +
                "]";
    }
}
