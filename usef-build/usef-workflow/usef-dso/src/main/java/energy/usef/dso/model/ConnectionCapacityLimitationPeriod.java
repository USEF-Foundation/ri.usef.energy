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

import energy.usef.core.model.Connection;

import java.math.BigInteger;
import java.util.Date;

import javax.persistence.*;

import org.joda.time.LocalDateTime;

/**
 *
 */

@Entity
@Table(name = "CONNECTION_CAPACITY_LIMITATION_PERIOD")
public class ConnectionCapacityLimitationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CONNECTION", nullable = false)
    private Connection connection;

    @Column(name = "START_DATETIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDateTime;

    @Column(name = "END_DATETIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDateTime;

    @Column(name = "TOTAL_OUTAGE", nullable = false)
    private Boolean totalOutage;

    @Column(name = "CAPACITY_REDUCTION")
    private BigInteger capacityReduction;

    public ConnectionCapacityLimitationPeriod() {
        //default constructor
    }

    public ConnectionCapacityLimitationPeriod(Connection connection, Date startDateTime, Date endDateTime, boolean totalOutage) {
        this(connection, startDateTime, endDateTime, totalOutage, null);
    }

    public ConnectionCapacityLimitationPeriod(Connection connection, Date startDateTime, Date endDateTime, boolean totalOutage,
            BigInteger capacity) {
        this.connection = connection;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.totalOutage = totalOutage;
        this.capacityReduction = capacity;
    }

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

    public Boolean isFullOutage() {
        return totalOutage;
    }

    public void setTotalOutage(Boolean totalOutage) {
        this.totalOutage = totalOutage;
    }

    public LocalDateTime getStartDateTime() {
        if (startDateTime == null) {
            return null;
        }
        return new LocalDateTime(startDateTime);
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        if (startDateTime == null) {
            this.startDateTime = null;
        } else {
            this.startDateTime = startDateTime.toDateTime().toDate();
        }
    }

    public LocalDateTime getEndDateTime() {
        if (endDateTime == null) {
            return null;
        }
        return new LocalDateTime(endDateTime);
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        if (endDateTime == null) {
            this.endDateTime = null;
        } else {
            this.endDateTime = endDateTime.toDateTime().toDate();
        }
    }

    public BigInteger getCapacityReduction() {
        return capacityReduction;
    }

    public void setCapacityReduction(BigInteger capacityReduction) {
        this.capacityReduction = capacityReduction;
    }

    @Override
    public String toString() {
        return "ConnectionCapacityLimitationPeriod" + "[" +
                "id=" + id +
                ", startDateTime=" + startDateTime +
                ", endDateTime=" + endDateTime +
                ", totalOutage=" + totalOutage +
                ", capacityReduction=" + capacityReduction +
                "]";
    }
}
