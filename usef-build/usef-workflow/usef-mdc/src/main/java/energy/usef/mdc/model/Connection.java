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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
/**
 * Entity class for the Connections on the MDC.
 */
@Entity
@Table(name = "CONNECTION")
public class Connection {

    @Id
    @Column(name = "ENTITY_ADDRESS", nullable = false)
    private String entityAddress;

    /**
     * Default constructor for JPA.
     */
    public Connection() {
        // empty constructor.
    }

    /**
     * Constructor for a Connection with its entity address.
     *
     * @param entityAddress {@link String} entity address.
     */
    public Connection(String entityAddress) {
        this.entityAddress = entityAddress;
    }

    public String getEntityAddress() {
        return entityAddress;
    }

    public void setEntityAddress(String entityAddress) {
        this.entityAddress = entityAddress;
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
        Connection other = (Connection) obj;
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
        return "Connection" + "[" +
                "entityAddress='" + entityAddress + "'" +
                "]";
    }
}
