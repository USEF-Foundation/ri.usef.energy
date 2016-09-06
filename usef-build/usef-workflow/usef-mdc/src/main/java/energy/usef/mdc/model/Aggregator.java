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
 * Entity class for the Aggregator.
 */
@Entity
@Table(name = "AGGREGATOR")
public class Aggregator {

    @Id
    @Column(name = "DOMAIN", nullable = false)
    private String domain;

    /**
     * Default constructor for JPA.
     */
    public Aggregator() {
        // empty constructor.
    }

    /**
     * Constructor with the domain name of the aggregator.
     *
     * @param domain {@link String} domain name.
     */
    public Aggregator(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
