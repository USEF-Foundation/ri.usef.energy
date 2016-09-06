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

import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * {@link PtuFlexRequest} entity class represents the connection on which an aggregator can have a customer.
 */
@Entity
@Table(name = "PTU_FLEXREQUEST")
@SequenceGenerator(name = "SEQUENCE_CONNECTION", sequenceName = "SEQUENCE_CONNECTION", initialValue = 1,
        allocationSize = 1)
public class PtuFlexRequest extends Exchange {

    @Column(name = "POWER", precision=18, scale=0, nullable = false)
    private BigInteger power;

    @Column(name = "DISPOSITION", nullable = false)
    @Enumerated(EnumType.STRING)
    private DispositionAvailableRequested disposition;

    @Column(name = "PROGNOSIS_SEQUENCE", nullable = true)
    private Long prognosisSequence;

    /**
     * Empty constructor for JPA.
     */
    public PtuFlexRequest() {
        super();
    }

    public BigInteger getPower() {
        return power;
    }

    public void setPower(BigInteger power) {
        this.power = power;
    }

    public DispositionAvailableRequested getDisposition() {
        return disposition;
    }

    public void setDisposition(DispositionAvailableRequested disposition) {
        this.disposition = disposition;
    }

    public Long getPrognosisSequence() {
        return prognosisSequence;
    }

    public void setPrognosisSequence(Long prognosisSequence) {
        this.prognosisSequence = prognosisSequence;
    }

    @Override
    public String toString() {
        return "PtuFlexRequest" + "[" +
                "power=" + power +
                ", disposition=" + disposition +
                ", prognosisSequence=" + prognosisSequence +
                "]";
    }
}
