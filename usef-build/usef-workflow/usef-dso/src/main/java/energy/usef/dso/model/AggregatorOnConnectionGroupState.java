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

import energy.usef.core.model.CongestionPointConnectionGroup;

import java.math.BigInteger;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;

/**
 * Entity with the state of the belonging of an aggregator on a connection group.
 */
@Entity
@Table(name = "AGGREGATOR_ON_CONNECTION_GROUP_STATE")
public class AggregatorOnConnectionGroupState {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "AGGREGATOR_DOMAIN", foreignKey = @ForeignKey(name = "AOCGS_AGGREGATOR_FK"), nullable = false)
    private Aggregator aggregator;

    @ManyToOne
    @JoinColumn(name = "CONGESTION_POINT_CONNECTION_GROUP_ID", foreignKey = @ForeignKey(name = "AOCGS_CPCG_FK"), nullable = false)
    private CongestionPointConnectionGroup congestionPointConnectionGroup;

    @Column(name = "CONNECTION_COUNT", precision = 18, scale = 0, nullable = false)
    private BigInteger connectionCount;

    @Column(name = "VALID_FROM", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date validFrom;

    @Column(name = "VALID_UNTIL", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date validUntil;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Aggregator getAggregator() {
        return aggregator;
    }

    public void setAggregator(Aggregator aggregator) {
        this.aggregator = aggregator;
    }

    public CongestionPointConnectionGroup getCongestionPointConnectionGroup() {
        return congestionPointConnectionGroup;
    }

    public void setCongestionPointConnectionGroup(CongestionPointConnectionGroup congestionPointConnectionGroup) {
        this.congestionPointConnectionGroup = congestionPointConnectionGroup;
    }

    public BigInteger getConnectionCount() {
        return connectionCount;
    }

    public void setConnectionCount(BigInteger connectionCount) {
        this.connectionCount = connectionCount;
    }

    public LocalDate getValidFrom() {
        if (validFrom == null) {
            return null;
        }
        return new LocalDate(validFrom);
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom == null ? null : validFrom.toDateMidnight().toDate();
    }

    public LocalDate getValidUntil() {
        if (validUntil == null) {
            return null;
        }
        return new LocalDate(validUntil);
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil == null ? null : validUntil.toDateMidnight().toDate();
    }

    @Override
    public String toString() {
        return "AggregatorOnConnectionGroupState" + "[" +
                "id=" + id +
                ", connectionCount=" + connectionCount +
                ", validFrom=" + validFrom +
                ", validUntil=" + validUntil +
                "]";
    }
}
