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

package energy.usef.cro.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Entity class {@link CongestionPoint}: This class is a representation of a CongestionPoint for the CRO role.
 */
@Entity
@Table(name = "CONGESTION_POINT")
public class CongestionPoint {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "ENTITY_ADDRESS", unique = true, nullable = false)
    private String entityAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DISTRIBUTION_SYSTEM_OPERATOR_ID", foreignKey = @ForeignKey(name = "CGP_DSO_FK"), nullable = false)
    private DistributionSystemOperator distributionSystemOperator;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "congestionPoint")
    private Set<Connection> connections;

    public CongestionPoint() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the Entity Address
     * 
     * @return an arbitrary String representing the Gridpoint's entity address.
     */
    public String getEntityAddress() {
        return entityAddress;
    }

    /**
     * Sets the Entity Address
     * 
     * @param entityAddress an arbitrary String representing the Gridpoint's entity address.
     */
    public void setEntityAddress(String entityAddress) {
        this.entityAddress = entityAddress;
    }

    public DistributionSystemOperator getDistributionSystemOperator() {
        return distributionSystemOperator;
    }

    public void setDistributionSystemOperator(
            DistributionSystemOperator distributionSystemOperator) {
        this.distributionSystemOperator = distributionSystemOperator;
    }

    public Set<Connection> getConnections() {
        if (connections == null) {
            connections = new HashSet<>();
        }
        return connections;
    }

    public void setConnections(Set<Connection> connections) {
        this.connections = connections;
    }

    @Override
    public String toString() {
        return "CongestionPoint" + "[" +
                "id=" + id +
                ", entityAddress='" + entityAddress + "'" +
                "]";
    }
}
