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

import energy.usef.environment.tool.config.ToolConfig;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleConfig.class);
    private static final String HOST_ROLE = "HOST_ROLE";
    private Role role;
    private String domain;
    private String datasource;
    private Map<String, String> config = new HashMap<>();

    /**
     * @param role
     * @param domain
     */
    public RoleConfig(Role role, String domain, Map<String, String> config, boolean perParticipantDatabase) {
        super();
        this.role = role;
        this.domain = domain;
        this.config = config;
        if (perParticipantDatabase) {
            datasource = (domain.replace(".", "_").replace("-", "_") + "_" + role.name()).toUpperCase()+ "_DS";
        } else {
            datasource = "USEF_DS";
        }

    }

    public void mergeConfiguration(Map<String, String> globalConfig) {
        for (Entry<String, String> entry : globalConfig.entrySet()) {
            if (config.get(entry.getKey()) == null) {
                config.put(entry.getKey(), entry.getValue());
            }
        }
        config.put(HOST_ROLE, this.role.name());
    }

    public void checkConfigurationBeforeMerge(Map<String, String> globalConfig) {
        if (config.get(UsefEnvironment.KEYSTORE_PASSWORD) != null &&
                config.get(UsefEnvironment.KEYSTORE_PASSWORD) != globalConfig.get(UsefEnvironment.KEYSTORE_PASSWORD)) {
            LOGGER.error(
                    "The keystore password can only be configured globally. Please, remove the domain specific key with name {}.",
                    UsefEnvironment.KEYSTORE_PASSWORD);
            System.exit(1);
        }
    }

    public String getDdlScript() {
        String ddlFilename = null;
        if (Role.AGR == role) {
            ddlFilename = ToolConfig.getUsefEnvironmentBuildAgrDdlFolder() + File.separator + ToolConfig.DDL_FILENAME;
        } else if (Role.BRP == role) {
            ddlFilename = ToolConfig.getUsefEnvironmentBuildBrpDdlFolder() + File.separator + ToolConfig.DDL_FILENAME;
        } else if (Role.CRO == role) {
            ddlFilename = ToolConfig.getUsefEnvironmentBuildCroDdlFolder() + File.separator + ToolConfig.DDL_FILENAME;
        } else if (Role.DSO == role) {
            ddlFilename = ToolConfig.getUsefEnvironmentBuildDsoDdlFolder() + File.separator + ToolConfig.DDL_FILENAME;
        } else if (Role.MDC == role) {
            ddlFilename = ToolConfig.getUsefEnvironmentBuildMdcDdlFolder() + File.separator + ToolConfig.DDL_FILENAME;
        } else {
            LOGGER.error("Could not find DDL filename for role {}.", role);
            System.exit(1);
        }
        return ddlFilename;
    }

    public String getWarFile() {
        String warFilename = null;
        if (Role.AGR == role) {
            warFilename = ToolConfig.getUsefEnvironmentBuildAgrWarFolder() + File.separator + ToolConfig.TARGET_AGR_WAR;
        } else if (Role.BRP == role) {
            warFilename = ToolConfig.getUsefEnvironmentBuildBrpWarFolder() + File.separator + ToolConfig.TARGET_BRP_WAR;
        } else if (Role.CRO == role) {
            warFilename = ToolConfig.getUsefEnvironmentBuildCroWarFolder() + File.separator + ToolConfig.TARGET_CRO_WAR;
        } else if (Role.DSO == role) {
            warFilename = ToolConfig.getUsefEnvironmentBuildDsoWarFolder() + File.separator + ToolConfig.TARGET_DSO_WAR;
        } else if (Role.MDC == role) {
            warFilename = ToolConfig.getUsefEnvironmentBuildMdcWarFolder() + File.separator + ToolConfig.TARGET_MDC_WAR;
        } else {
            LOGGER.error("Could not find WAR filename for role {}.", role);
            System.exit(1);
        }
        return warFilename;
    }

    public String getUniqueName() {
        return domain.toLowerCase() + "_" + role.name();
    }

    public String getUniqueDbSchemaName() {
        return (domain.replace(".", "_").replace("-", "_") + "_" + role.name()).toUpperCase();
    }

    public String getUniqueDatasourceName() {
        return datasource;
    }

    public String getTemplateDbSchemaName() {
      return (role.name() + "_USEF_EXAMPLE_COM_" + role.name()).toUpperCase();
    }

    public Role getRole() {
        return role;
    }

    public String getDomain() {
        return domain.toLowerCase();
    }

    public Map<String, String> getConfig() {
        return config;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
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
        RoleConfig other = (RoleConfig) obj;
        if (domain == null) {
            if (other.domain != null)
                return false;
        } else if (!domain.equals(other.domain))
            return false;
        if (role != other.role)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RoleConfig [role=" + role + ", domain=" + domain + "]";
    }

}
