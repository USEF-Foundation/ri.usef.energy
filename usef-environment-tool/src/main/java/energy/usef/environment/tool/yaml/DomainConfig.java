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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DomainConfig {
    private String domain;
    private Map<Role, RoleConfig> roleConfigs = new HashMap<>();
    private Map<String, String> config = new HashMap<>();

    public RoleConfig getRoleConfig(Role role) {
        return roleConfigs.get(role);
    }

    public void add(RoleConfig roleConfig) {
        roleConfigs.put(roleConfig.getRole(), roleConfig);
    }

    public Set<Role> getRoles() {
        return roleConfigs.keySet();
    }

    public Collection<RoleConfig> getRoleConfigs() {
        return roleConfigs.values();
    }

    /**
     * @param domain
     */
    public DomainConfig(String domain) {
        super();
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public Map<String, String> getConfig() {
        return config;
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DomainConfig other = (DomainConfig) obj;
        if (domain == null) {
            if (other.domain != null)
                return false;
        } else if (!domain.equals(other.domain))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DomainConfig [domain=" + domain + "]";
    }

}
