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
 * Subclass of {@link ConnectionGroup} representing connections grouped with a BRP.
 */
@Entity
@DiscriminatorValue("BRP")
public class BrpConnectionGroup extends ConnectionGroup {

    @Column(name = "BRP_DOMAIN")
    private String brpDomain;

    /**
     * Default constructor.
     */
    public BrpConnectionGroup() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public BrpConnectionGroup(String usefIdentifier) {
        super(usefIdentifier);
    }

    public String getBrpDomain() {
        return brpDomain;
    }

    public void setBrpDomain(String brpDomain) {
        this.brpDomain = brpDomain;
    }

    @Override
    public String toString() {
        return "BrpConnectionGroup" + "[" +
                "brpDomain='" + brpDomain + "'" +
                "]";
    }
}
