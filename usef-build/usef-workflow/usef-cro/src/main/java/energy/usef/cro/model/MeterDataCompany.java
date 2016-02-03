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

package energy.usef.cro.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity class {@link MeterDataCompany}: This class is a representation of an MeterDataCompany for the CRO role.
 */
@Entity
@Table(name = "METER_DATA_COMPANY")
public class MeterDataCompany {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "DOMAIN", unique = true, nullable = false)
    private String domain;

    /***
     * Empty constructor used by JPA to create instances.
     */
    public MeterDataCompany() {
    }

    /***
     * Create an aggregator instance with a domain.
     * 
     * @param domain the domain the aggregator is connected to.
     */
    public MeterDataCompany(String domain) {
        this.domain = domain;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String toString() {
        return "MeterDataCompany" + "[" +
                "id=" + id +
                ", domain='" + domain + "'" +
                "]";
    }
}
