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

package energy.usef.dso.model;

import java.math.BigInteger;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity class to record updated prognosis with deviation.
 */
@Entity
@Table(name = "PROGNOSIS_UPDATE_DEVIATION")
public class PrognosisUpdateDeviation {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "PROGNOSIS_SEQUENCE", nullable = false)
    private Long prognosisSequence;

    @Column(name = "PTU_DATE", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date ptuDate;

    @Column(name = "PTU_INDEX", nullable = false)
    private Integer ptuIndex;

    @Column(name = "AGGREGATOR_DOMAIN", nullable = false)
    private String aggregatorDomain;

    @Column(name = "ORDERED_POWER", precision = 18, scale = 0, nullable = false)
    private BigInteger orderedPower;

    @Column(name = "PREVIOUS_PROGNOSED_POWER", precision = 18, scale = 0, nullable = false)
    private BigInteger previousPrognosedPower;

    @Column(name = "PROGNOSED_POWER", precision = 18, scale = 0, nullable = false)
    private BigInteger prognosedPower;

    /**
     * Default constructor for JPA.
     */
    public PrognosisUpdateDeviation() {
        super();
    }

    /**
     * Constructor with the main fields to fill in to persist the entity.
     * 
     * @param prognosisSequence {@link Long} sequence number of the updated prognosis.
     * @param aggregatorDomain {@link String} domain name of the related aggregator.
     * @param ptuDate {@link Date} period of the prognosis.
     * @param ptuIndex {@link Integer} index of the related ptu.
     * @param orderedPower {@link BigInteger} ordered power (sum of the ACCEPTED orders related to the previous ptu).
     * @param prognosedPower {@link BigInteger} power prognosed in the updated prognosis.
     */
    public PrognosisUpdateDeviation(Long prognosisSequence, String aggregatorDomain, Date ptuDate, Integer ptuIndex,
            BigInteger orderedPower, BigInteger previousPrognosedPower, BigInteger prognosedPower) {
        super();
        this.prognosisSequence = prognosisSequence;
        this.aggregatorDomain = aggregatorDomain;
        this.ptuDate = (ptuDate == null ? null : (Date) ptuDate.clone());
        this.ptuIndex = ptuIndex;
        this.orderedPower = orderedPower;
        this.previousPrognosedPower = previousPrognosedPower;
        this.prognosedPower = prognosedPower;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrognosisSequence() {
        return prognosisSequence;
    }

    public void setPrognosisSequence(Long prognosisSequence) {
        this.prognosisSequence = prognosisSequence;
    }

    public String getAggregatorDomain() {
        return aggregatorDomain;
    }

    public Date getPtuDate() {
        return ptuDate;
    }

    public void setPtuDate(Date ptuDate) {
        this.ptuDate = ptuDate;
    }

    public Integer getPtuIndex() {
        return ptuIndex;
    }

    public void setPtuIndex(Integer ptuIndex) {
        this.ptuIndex = ptuIndex;
    }

    public void setAggregatorDomain(String aggregatorDomain) {
        this.aggregatorDomain = aggregatorDomain;
    }

    public BigInteger getOrderedPower() {
        return orderedPower;
    }

    public void setOrderedPower(BigInteger orderedPower) {
        this.orderedPower = orderedPower;
    }

    public BigInteger getPreviousPrognosedPower() {
        return previousPrognosedPower;
    }

    public void setPreviousPrognosedPower(BigInteger previousPrognosedPower) {
        this.previousPrognosedPower = previousPrognosedPower;
    }

    public BigInteger getPrognosedPower() {
        return prognosedPower;
    }

    public void setPrognosedPower(BigInteger prognosedPower) {
        this.prognosedPower = prognosedPower;
    }

    @Override
    public String toString() {
        return "PrognosisUpdateDeviation" + "[" +
                "id=" + id +
                ", prognosisSequence=" + prognosisSequence +
                ", ptuDate=" + ptuDate +
                ", ptuIndex=" + ptuIndex +
                ", aggregatorDomain='" + aggregatorDomain + "'" +
                ", orderedPower=" + orderedPower +
                ", previousPrognosedPower=" + previousPrognosedPower +
                ", prognosedPower=" + prognosedPower +
                "]";
    }
}
