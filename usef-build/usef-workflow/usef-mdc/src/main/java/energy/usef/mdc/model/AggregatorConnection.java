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

package energy.usef.mdc.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
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
 * Entity representing the assocation of an {@link Aggregator} on a {@link Connection}.
 */
@Entity
@Table(name = "AGGREGATOR_CONNECTION")
public class AggregatorConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CONNECTION_ENTITY_ADDRESS", nullable = false)
    private Connection connection;

    @ManyToOne
    @JoinColumn(name = "AGGREGATOR_DOMAIN", nullable = false)
    private Aggregator aggregator;

    @ManyToOne
    @JoinColumn(name = "CRO_DOMAIN", nullable = false)
    private CommonReferenceOperator commonReferenceOperator;

    @Column(name = "VALID_FROM", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date validFrom;

    @Column(name = "VALID_UNTIL", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date validUntil;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Aggregator getAggregator() {
        return aggregator;
    }

    public void setAggregator(Aggregator aggregator) {
        this.aggregator = aggregator;
    }

    public CommonReferenceOperator getCommonReferenceOperator() {
        return commonReferenceOperator;
    }

    public void setCommonReferenceOperator(CommonReferenceOperator commonReferenceOperator) {
        this.commonReferenceOperator = commonReferenceOperator;
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
}
