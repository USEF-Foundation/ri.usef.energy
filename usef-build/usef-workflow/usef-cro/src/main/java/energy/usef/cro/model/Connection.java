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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity class {@link Connection}: This class is a representation of a Connection for the CRO role.
 */
@Entity
@Table(name = "CONNECTION")
public class Connection {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "ENTITY_ADDRESS", unique = true, nullable = false)
    private String entityAddress;

    @ManyToOne
    @JoinColumn(name = "CONGESTION_POINT_ID", foreignKey = @ForeignKey(name = "CNP_CGP_FK"), nullable = true)
    private CongestionPoint congestionPoint;

    @ManyToOne
    @JoinColumn(name = "AGGREGATOR_ID", foreignKey = @ForeignKey(name = "CNP_AGR_FK"), nullable = true)
    private Aggregator aggregator;

    @ManyToOne
    @JoinColumn(name = "BALANCE_RESPONSIBLE_PARTY_ID", foreignKey = @ForeignKey(name = "CNP_BRP_FK"), nullable = true)
    private BalanceResponsibleParty balanceResponsibleParty;

    /**
     * Constructs a default connection.
     * 
     */
    public Connection() {
    }

    /**
     * Constructs a connection with the specified entity address.
     * 
     * @param entityAddress.
     *
     */
    public Connection(String entityAddress) {
        this.entityAddress = entityAddress;
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

    public CongestionPoint getCongestionPoint() {
        return congestionPoint;
    }

    public void setCongestionPoint(CongestionPoint congestionPoint) {
        this.congestionPoint = congestionPoint;
    }

    public Aggregator getAggregator() {
        return aggregator;
    }

    public void setAggregator(Aggregator aggregator) {
        this.aggregator = aggregator;
    }

    public BalanceResponsibleParty getBalanceResponsibleParty() {
        return balanceResponsibleParty;
    }

    public void setBalanceResponsibleParty(BalanceResponsibleParty balanceResponsibleParty) {
        this.balanceResponsibleParty = balanceResponsibleParty;
    }

    @Override
    public String toString() {
        return "Connection" + "[" +
                "id=" + id +
                ", entityAddress='" + entityAddress + "'" +
                "]";
    }
}
