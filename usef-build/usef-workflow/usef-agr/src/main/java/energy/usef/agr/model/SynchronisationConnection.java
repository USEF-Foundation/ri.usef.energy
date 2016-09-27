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

package energy.usef.agr.model;

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
@Table(name = "SYNCHRONISATION_CONNECTION")
public class SynchronisationConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "ENTITY_ADDRESS", nullable = false, unique = true)
    private String entityAddress;

    @Column(name = "IS_CUSTOMER", nullable = false)
    private boolean isCustomer;

    @Column(name = "LAST_SYNCHRONISATION_TIME", nullable = true)
    private Date lastSynchronisationTime;

    @Column(name = "LAST_MODIFICATION_TIME", nullable = false)
    private Date lastModificationTime;

    @OneToMany(mappedBy = "synchronisationConnection")
    private List<SynchronisationConnectionStatus> statusses;

    @Transient
    public SynchronisationConnectionStatusType findStatusForCRO(String domain) {
        for (SynchronisationConnectionStatus status : getStatusses()) {
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

    public boolean isCustomer() {
        return isCustomer;
    }

    public void setCustomer(boolean isCustomer) {
        this.isCustomer = isCustomer;
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

    public List<SynchronisationConnectionStatus> getStatusses() {
        if (statusses == null) {
            statusses = new ArrayList<>();
        }
        return statusses;
    }

    public void setStatusses(List<SynchronisationConnectionStatus> statusses) {
        this.statusses = statusses;
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
        SynchronisationConnection other = (SynchronisationConnection) obj;
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
        return "SynchronisationConnection" + "[" +
                "id=" + id +
                ", entityAddress='" + entityAddress + "'" +
                ", isCustomer=" + isCustomer +
                ", lastSynchronisationTime=" + lastSynchronisationTime +
                ", lastModificationTime=" + lastModificationTime +
                "]";
    }
}
