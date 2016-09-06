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
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * This repository is used to manage {@link AgrConnectionGroup}.
 */
@Entity
@DiscriminatorValue("AGR")
public class AgrConnectionGroup extends ConnectionGroup {

    @Column(name = "AGR_DOMAIN", unique = true)
    private String aggregatorDomain;

    /**
     * Default constructor.
     */
    public AgrConnectionGroup() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public AgrConnectionGroup(String usefIdentifier) {
        super(usefIdentifier);
    }

    /**
     * @return the aggregatorDomain
     */
    public String getAggregatorDomain() {
        return aggregatorDomain;
    }

    /**
     * @param aggregatorDomain the aggregatorDomain to set
     */
    public void setAggregatorDomain(String aggregatorDomain) {
        this.aggregatorDomain = aggregatorDomain;
    }

    @Override
    public String toString() {
        return "AgrConnectionGroup" + "[" +
                "aggregatorDomain='" + aggregatorDomain + "'" +
                "]";
    }
}
