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
 * JPA Entity for storing ConnectionMeterEvent's.
 */
@Entity
@Table(name = "CONNECTION_METER_EVENT")
public class ConnectionMeterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CONNECTION", nullable = false)
    private Connection connection;

    @Column(name = "EVENT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "DATETIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateTime;

    @Column(name = "CAPACITY")
    private BigInteger capacity;

    /**
     * Default Constructor
     */
    public ConnectionMeterEvent() {
        //default constructor
    }

    /**
     * Constructor with all required fields.
     *
     * @param connection
     * @param eventType
     * @param dateTime
     */
    public ConnectionMeterEvent(Connection connection, EventType eventType, LocalDateTime dateTime) {
        this(connection, eventType, dateTime, null);
    }

    /**
     * Constructor with all fields.
     *
     * @param connection
     * @param eventType
     * @param dateTime
     * @param capacity
     */
    public ConnectionMeterEvent(Connection connection, EventType eventType, LocalDateTime dateTime, BigInteger capacity) {
        this.connection = connection;
        this.eventType = eventType;
        setDateTime(dateTime);
        this.capacity = capacity;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getDateTime() {
        if (dateTime == null) {
            return null;
        }
        return new LocalDateTime(dateTime);
    }

    public void setDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            this.dateTime = null;
        } else {
            this.dateTime = dateTime.toDateTime().toDate();
        }
    }

    public BigInteger getCapacity() {
        return capacity;
    }

    public void setCapacity(BigInteger capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return "ConnectionMeterEvent" + "[" +
                "id=" + id +
                ", dateTime=" + dateTime +
                ", capacity=" + capacity +
                "]";
    }
}
