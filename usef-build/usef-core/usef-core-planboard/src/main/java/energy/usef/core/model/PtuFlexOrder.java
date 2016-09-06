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

package energy.usef.core.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * Entity class {@link PtuFlexOrder}: This class is a representation of a FlexOrder.
 *
 */
@Entity
@Table(name = "PTU_FLEXORDER")
public class PtuFlexOrder extends Exchange {

    @Column(name = "FLEXOFFER_SEQUENCE", nullable = true)
    private Long flexOfferSequence;

    @Column(name = "ACKNOWLEDGEMENT_STATUS", columnDefinition = "varchar(255) default 'SENT' not null")
    @Enumerated(EnumType.STRING)
    private AcknowledgementStatus acknowledgementStatus;

    public Long getFlexOfferSequence() {
        return flexOfferSequence;
    }

    public void setFlexOfferSequence(Long flexOfferSequence) {
        this.flexOfferSequence = flexOfferSequence;
    }

    public AcknowledgementStatus getAcknowledgementStatus() {
        return acknowledgementStatus;
    }

    public void setAcknowledgementStatus(AcknowledgementStatus acknowledgementStatus) {
        this.acknowledgementStatus = acknowledgementStatus;
    }

    @Override
    public String toString() {
        return "PtuFlexOrder" + "[" +
                "flexOfferSequence=" + flexOfferSequence +
                ", acknowledgementStatus=" + acknowledgementStatus +
                "]";
    }
}
