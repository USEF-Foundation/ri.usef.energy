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

package energy.usef.core.data.participant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Information on a specific participant.
 */
public class Participant {

    private String domainName;
    private String specVersion;
    private List<ParticipantRole> roles;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public void setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
    }

    public List<ParticipantRole> getRoles() {
        return roles;
    }

    public void setRoles(List<ParticipantRole> roles) {
        this.roles = roles;
    }

    /**
     * Add participant roles to the {@link Participant}.
     *
     * @param participantRoles
     */
    public void addParticipantRoles(ParticipantRole... participantRoles) {
        if (participantRoles == null || participantRoles.length == 0) {
            return;
        }
        if (this.roles == null) {
            this.roles = new ArrayList<>();
        }
        Collections.addAll(this.roles, participantRoles);
    }

}
