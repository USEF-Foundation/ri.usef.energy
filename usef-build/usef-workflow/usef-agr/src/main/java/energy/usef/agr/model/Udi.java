/*
 * Copyright 2015 USEF Foundation
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

import energy.usef.core.model.Connection;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
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
import javax.persistence.UniqueConstraint;

/**
 * {@link Udi} entity class represents the UDI device which forecasts have been received.
 */
@Entity
@Table(name = "UDI", uniqueConstraints = @UniqueConstraint(columnNames = { "endpoint" }, name = "UDI_UK"))
public class Udi {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Column(name = "DTU_SIZE", nullable = false)
    private Integer dtuSize;

    @Column(name = "ENDPOINT", nullable = false, unique = true)
    private String endpoint;

    @Column(name = "PROFILE")
    private String profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONNECTION_ID", foreignKey = @ForeignKey(name = "UDI_CON_FK"), nullable = false)
    private Connection connection;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "udi", cascade = CascadeType.ALL)
    private List<UdiEvent> udiEvents;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDtuSize() {
        return dtuSize;
    }

    public void setDtuSize(Integer dtuSize) {
        this.dtuSize = dtuSize;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * @return the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint the endpoint to set
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public List<UdiEvent> getUdiEvents() {
        if (udiEvents == null) {
            udiEvents = new ArrayList<>();
        }
        return udiEvents;
    }

    @Override
    public String toString() {
        return "Udi" + "[" +
                "id=" + id +
                ", dtuSize=" + dtuSize +
                ", endpoint='" + endpoint + "'" +
                "]";
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
