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

package energy.usef.environment.tool.yaml;

public enum Role {
    CRO("cro-role", "CRO_USEF_EXAMPLE_COM_CRO"),
    DSO("dso-role", "DSO_USEF_EXAMPLE_COM_DSO"),
    AGR("agr-role", "AGR_USEF_EXAMPLE_COM_AGR"),
    BRP("brp-role", "BRP_USEF_EXAMPLE_COM_BRP"),
    MDC("mdc-role", "MDC_USEF_EXAMPLE_COM_MDC");

    private String roleNameInParticipantsYaml;
    private String defaultDBName;

    /**
     * @param roleNameInParticipantsYaml
     */
    private Role(String roleNameInParticipantsYaml, String defaultDBName) {
        this.roleNameInParticipantsYaml = roleNameInParticipantsYaml;
        this.defaultDBName = defaultDBName;
    }

    public String getRoleNameInParticipantsYaml() {
        return roleNameInParticipantsYaml;
    }

    public String getDefaultDBName() {
        return defaultDBName;
    }

}
