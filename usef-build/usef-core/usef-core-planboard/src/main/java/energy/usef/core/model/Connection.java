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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Connection entity for the planboard.
 */
@Entity
@Table(name = "CONNECTION")
public class Connection {

    @Id
    @Column(name = "ENTITY_ADDRESS")
    private String entityAddress;

    public Connection() {
        // do nothing
    }

    /**
     * Constructor with a specific entity address for the Connection.
     *
     * @param entityAddress {@link String} entity address of the Connection.
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
    public boolean equals(Object connection) {
        if (connection == null || !(connection instanceof Connection)) {
            return false;
        }
        if (connection == this) {
            return true;
        }
        if (((Connection) connection).getEntityAddress() == null || this.getEntityAddress() == null) {
            return false;
        }
        return ((Connection) connection).getEntityAddress().equals(this.getEntityAddress());
    }

    @Override
    public int hashCode() {
        return entityAddress.hashCode();
    }

    @Override
    public String toString() {
        return "Connection" + "[" +
                "entityAddress='" + entityAddress + "'" +
                "]";
    }
}
