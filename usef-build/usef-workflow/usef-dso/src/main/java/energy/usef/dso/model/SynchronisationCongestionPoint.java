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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.joda.time.LocalDateTime;

/**
 * Entity for the SYNCHRONISATION_CONNECTION Table.
 */
@Entity
@Table(name = "SYNCHRONISATION_CONGESTION_POINT")
public class SynchronisationCongestionPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "ENTITY_ADDRESS", nullable = false, unique = true)
    private String entityAddress;

    @Column(name = "LAST_SYNCHRONISATION_TIME", nullable = true)
    private Date lastSynchronisationTime;

    @Column(name = "LAST_MODIFICATION_TIME", nullable = false)
    private Date lastModificationTime;

    @OneToMany(mappedBy = "synchronisationCongestionPoint")
    private List<SynchronisationCongestionPointStatus> statusses;

    @OneToMany(mappedBy = "congestionPoint")
    private List<SynchronisationConnection> connections;

    @Transient
    public SynchronisationConnectionStatusType findStatusForCRO(String domain) {
        for (SynchronisationCongestionPointStatus status : getStatusses()) {
            if (domain.equals(status.getCommonReferenceOperator().getDomain()))
                return status.getStatus();
        }
        return null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityAddress() {
        return entityAddress;
    }

    public void setEntityAddress(String entityAddress) {
        this.entityAddress = entityAddress;
    }

    public LocalDateTime getLastSynchronisationTime() {
        if (lastSynchronisationTime == null) {
            return null;
        }
        return new LocalDateTime(lastSynchronisationTime);
    }

    public void setLastSynchronisationTime(LocalDateTime lastSynchronisationTime) {
        if (lastSynchronisationTime == null) {
            this.lastSynchronisationTime = null;
        } else {
            this.lastSynchronisationTime = lastSynchronisationTime.toDateTime().toDate();
        }
    }

    public LocalDateTime getLastModificationTime() {
        if (lastModificationTime == null) {
            return null;
        }
        return new LocalDateTime(lastModificationTime);
    }

    public void setLastModificationTime(LocalDateTime lastModificationTime) {
        if (lastModificationTime == null) {
            this.lastModificationTime = null;
        } else {
            this.lastModificationTime = lastModificationTime.toDateTime().toDate();
        }
    }

    public List<SynchronisationCongestionPointStatus> getStatusses() {
        if (statusses == null) {
            statusses = new ArrayList<>();
        }
        return statusses;
    }

    public void setStatusses(List<SynchronisationCongestionPointStatus> statusses) {
        this.statusses = statusses;
    }

    public List<SynchronisationConnection> getConnections() {
        if (connections == null) {
            connections = new ArrayList<>();
        }
        return connections;
    }

    public void setConnections(List<SynchronisationConnection> connections) {
        this.connections = connections;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entityAddress == null) ? 0 : entityAddress.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SynchronisationCongestionPoint other = (SynchronisationCongestionPoint) obj;
        if (entityAddress == null) {
            if (other.entityAddress != null) {
                return false;
            }
        } else if (!entityAddress.equals(other.entityAddress)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SynchronisationCongestionPoint" + "[" +
                "id=" + id +
                ", entityAddress='" + entityAddress + "'" +
                ", lastSynchronisationTime=" + lastSynchronisationTime +
                ", lastModificationTime=" + lastModificationTime +
                "]";
    }
}
