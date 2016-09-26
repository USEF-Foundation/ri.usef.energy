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
 * Entity class {@link BalanceResponsibleParty}: This class is a representation of a BalanceResponsibleParty for the CRO role.
 */
@Entity
@Table(name = "BALANCE_RESPONSIBLE_PARTY")
public class BalanceResponsibleParty {

    @Id
    @Column(name = "DOMAIN", unique = true, nullable = false)
    private String domain;

    /**
     * Construct a default {@link BalanceResponsibleParty}.
     */
    public BalanceResponsibleParty() {
    }

    /**
     * Construct a default {@link BalanceResponsibleParty} with the specified domain.
     *
     * @param domain
     */
    public BalanceResponsibleParty(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
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
        BalanceResponsibleParty other = (BalanceResponsibleParty) obj;
        if (domain == null) {
            if (other.domain != null) {
                return false;
            }
        } else if (!domain.equals(other.domain)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "BalanceResponsibleParty [domain=" + domain + "]";
    }

}
