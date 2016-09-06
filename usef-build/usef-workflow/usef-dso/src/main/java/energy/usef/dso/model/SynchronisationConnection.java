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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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

    @ManyToOne
    @JoinColumn(name = "SYNCHRONISATION_CONGESTION_POINT_ID", nullable = false)
    private SynchronisationCongestionPoint congestionPoint;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SynchronisationCongestionPoint getCongestionPoint() {
        return congestionPoint;
    }

    public void setCongestionPoint(SynchronisationCongestionPoint congestionPoint) {
        this.congestionPoint = congestionPoint;
    }

    public String getEntityAddress() {
        return entityAddress;
    }

    public void setEntityAddress(String entityAddress) {
        this.entityAddress = entityAddress;
    }

    @Override
    public String toString() {
        return "SynchronisationConnection" + "[" +
                "id=" + id +
                ", entityAddress='" + entityAddress + "'" +
                "]";
    }
}
