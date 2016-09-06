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
 * Join table between {@link ConnectionGroup} and {@link Connection}.
 */
@Entity
@Table(name = "CONNECTION_GROUP_STATE")
public class ConnectionGroupState {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CONNECTION_GROUP_ID", foreignKey = @ForeignKey(name = "CGS_CONNECTION_GROUP_FK"), nullable = false)
    private ConnectionGroup connectionGroup;

    @ManyToOne
    @JoinColumn(name = "CONNECTION_ID", foreignKey = @ForeignKey(name = "CGS_CONNECTION_FK"), nullable = false)
    private Connection connection;

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

    public ConnectionGroup getConnectionGroup() {
        return connectionGroup;
    }

    public void setConnectionGroup(ConnectionGroup connectionGroup) {
        this.connectionGroup = connectionGroup;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public LocalDate getValidFrom() {
        if (validFrom == null) {
            return null;
        }
        return new LocalDate(validFrom);
    }

    public void setValidFrom(LocalDate validFrom) {
        if (validFrom == null) {
            this.validFrom = null;
        } else {
            this.validFrom = validFrom.toDateMidnight().toDate();
        }
    }

    public LocalDate getValidUntil() {
        if (validUntil == null) {
            return null;
        }
        return new LocalDate(validUntil);
    }

    public void setValidUntil(LocalDate validUntil) {
        if (validUntil == null) {
            this.validUntil = null;
        } else {
            this.validUntil = validUntil.toDateMidnight().toDate();
        }
    }

    @Override
    public String toString() {
        return "ConnectionGroupState" + "[" +
                "id=" + id +
                ", validFrom=" + validFrom +
                ", validUntil=" + validUntil +
                "]";
    }
}
