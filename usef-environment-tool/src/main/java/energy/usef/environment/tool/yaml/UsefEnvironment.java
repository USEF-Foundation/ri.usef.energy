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
import energy.usef.environment.tool.util.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsefEnvironment {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsefEnvironment.class);

    private static final String DATABASE_PER_PARTICIPANT = "per_participant_database";
    private static final String PROCESSES = "processes";
    private static final String TIME = "timeserver";
    private static final String DOMAIN_NAME = "domain-name";
    private static final String NODES = "nodes";
    private static final String NODE_NAME = "node";
    private static final String ADDRESS = "address";
    private static final String BASEPATH = "basepath";
    private static final String PATHSEP = "pathsep";

    private static final String DSO_ROLE = "dso-role";
    private static final String MDC_ROLE = "mdc-role";
    private static final String CRO_ROLE = "cro-role";
    private static final String BRP_ROLE = "brp-role";
    private static final String AGR_ROLE = "agr-role";

    public static final String KEYSTORE_PASSWORD = "keystore_password";
    public static final String KEYSTORE_PRIVATE_KEY_PASSWORD = "keystore_private_key_password";

    private Map<String, String> globalConfig = new HashMap<>();
    private Map<String, NodeConfig> nodes = new HashMap<>();
    private Map<String, Object> timeServerConfig = new HashMap<>();

    @SuppressWarnings("unchecked")
    public void load(String configFile) throws FileNotFoundException, IOException {
        parse(FileUtil.loadYaml(configFile));

        // merge the global configuration to the domain-role specific configuration. The domain-role specific configuration
        // is leading.
        mergeConfigurationToRoleSpecific();
    }

    private void mergeConfigurationToRoleSpecific() {
        for (NodeConfig nodeConfig : nodes.values()) {
            LOGGER.debug("merging configuration of node {}", nodeConfig.getNode());
            for (DomainConfig domain : nodeConfig.getDomainConfigs()) {
                for (RoleConfig roleConfig : domain.getRoleConfigs()) {
                    roleConfig.checkConfigurationBeforeMerge(globalConfig);
                    roleConfig.mergeConfiguration(globalConfig);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parse(Map<String, Object> config) {
        for (Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof String) {
                globalConfig.put((String) entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                globalConfig.put((String) entry.getKey(), ((Integer) entry.getValue()).toString());
            } else if (entry.getValue() instanceof Boolean) {
                globalConfig.put((String) entry.getKey(), ((Boolean) entry.getValue()).toString());
            } else if (entry.getValue() instanceof Double) {
                globalConfig.put((String) entry.getKey(), ((Double) entry.getValue()).toString());
            } else if (entry.getValue() instanceof Float) {
                globalConfig.put((String) entry.getKey(), ((Float) entry.getValue()).toString());
            } else if (entry.getValue() instanceof ArrayList) {
                if (NODES.equalsIgnoreCase(entry.getKey())) {
                    parseNodes((List<Map<String, Object>>) entry.getValue());
                } else {
                    LOGGER.error("Parent key should be 'processes'.");
                }
            } else if (entry.getValue() instanceof Map) {
                if (TIME.equalsIgnoreCase(entry.getKey())) {
                    parseTimeServer((Map<String, String>) entry.getValue());
                } else {
                    LOGGER.error("Parent key should be 'timeserver'.");
                }
            } else {
                LOGGER.warn("Unknow type found in configuration: " + entry.getValue());
            }
        }
    }

    private void parseTimeServer(Map<String, String> timeSettings) {
        timeServerConfig.putAll(timeSettings);
    }

    @SuppressWarnings("unchecked")
    private void parseDomains(NodeConfig nodeConfig, List<Map<String, Object>> domains) {
        for (Map<String, Object> domain : domains) {
            String domainName = (String) domain.get(DOMAIN_NAME);
            if (StringUtils.isEmpty(domainName)) {
                LOGGER.error("Under the parent key '" + PROCESSES + "', the key '" + DOMAIN_NAME + "' can not be found.");
                System.exit(1);
            }

            for (Entry<String, Object> roleElement : domain.entrySet()) {
                if (roleElement.getValue() instanceof Map) {
                    Map<String, Object> config = (Map<String, Object>) roleElement.getValue();
                    Role role = getRoleFromElement(roleElement);
                    parseDomainRole(this, nodeConfig, domainName, role, config);
                } else if (roleElement.getValue() == null) {
                    // empty role with no configuration items.
                    Role role = getRoleFromElement(roleElement);
                    parseDomainRole(this, nodeConfig, domainName, role, new HashMap<>());
                }
            }

        }
    }

    @SuppressWarnings("unchecked")
    private void parseNodes(List<Map<String, Object>> nodes) {
        for (Map<String, Object> node : nodes) {
            String nodeName = (String) node.get(NODE_NAME);
            if (StringUtils.isEmpty(nodeName)) {
                LOGGER.error("Under the parent key '" + NODES + "', the key '" + NODE_NAME + "' can not be found.");
                System.exit(1);
            }
            String nodeAddress = (String) node.get(ADDRESS);
            if (StringUtils.isEmpty(nodeAddress)) {
                LOGGER.error("Under the parent key '" + NODES + "', the key '" + ADDRESS + "' can not be found.");
                System.exit(1);
            }
            String basePath = (String) node.get(BASEPATH);
            if (StringUtils.isEmpty(basePath)) {
                basePath = ToolConfig.getUsefRootFolder();
            }
            String pathSep = (String) node.get(PATHSEP);
            if (StringUtils.isEmpty(pathSep)) {
                pathSep = File.separator;
            }
            NodeConfig nodeConfig = new NodeConfig(nodeName, nodeAddress, basePath, pathSep);
            this.nodes.put(nodeConfig.getNode(), nodeConfig);
            List<Map<String, Object>> processes = (List<Map<String, Object>>) node.get(PROCESSES);
            if (processes == null || processes.size() <= 0) {
                LOGGER.error("Under the parent key '" + NODES + "', the key '" + PROCESSES + "' can not be found.");
                System.exit(1);
            }

            parseDomains(nodeConfig, processes);
        }
    }

    private Role getRoleFromElement(Entry<String, Object> roleElement) {
        Role role = null;
        if (AGR_ROLE.equalsIgnoreCase(roleElement.getKey())) {
            role = Role.AGR;
        } else if (BRP_ROLE.equalsIgnoreCase(roleElement.getKey())) {
            role = Role.BRP;
        } else if (CRO_ROLE.equalsIgnoreCase(roleElement.getKey())) {
            role = Role.CRO;
        } else if (DSO_ROLE.equalsIgnoreCase(roleElement.getKey())) {
            role = Role.DSO;
        } else if (MDC_ROLE.equalsIgnoreCase(roleElement.getKey())) {
            role = Role.MDC;
        } else {
            LOGGER.error("Invalid role key for 'domain-name', allowed keys are 'agr-role', 'brp-role', 'cro-role', 'mdc-role' "
                    + "and 'dso-role'.");
            System.exit(1);
        }
        return role;
    }

    private void parseDomainRole(UsefEnvironment environmentConfig, NodeConfig nodeConfig, String domainName, Role role, Map<String, Object> config) {
        LOGGER.debug("Parsing configuration for node {}, domain {} and role {}.", nodeConfig.getNode(), domainName, role);
      
        DomainConfig domainConfig = nodeConfig.getDomain(domainName);
        if (domainConfig == null) {
            domainConfig = new DomainConfig(domainName);
            nodeConfig.add(domainConfig);
        }

        Map<String, String> configValues = new HashMap<>();
        for (Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof String) {
                configValues.put((String) entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                configValues.put((String) entry.getKey(), ((Integer) entry.getValue()).toString());
            } else if (entry.getValue() instanceof Boolean) {
                configValues.put((String) entry.getKey(), ((Boolean) entry.getValue()).toString());
            } else if (entry.getValue() instanceof Float) {
                configValues.put((String) entry.getKey(), ((Float) entry.getValue()).toString());
            } else if (entry.getValue() instanceof Double) {
                configValues.put((String) entry.getKey(), ((Double) entry.getValue()).toString());
            } else {
                LOGGER.warn("Unknown value in configuration. It won't be added to the configuration: {} with value {}",
                        entry.getKey(), entry.getValue());
            }
        }

        RoleConfig roleConfig = domainConfig.getRoleConfig(role);
        if (roleConfig == null) {
            domainConfig.add(new RoleConfig(role, domainName, configValues, environmentConfig.isDatabasePerParticipant()));
        } else {
            LOGGER.error("There is already an configuration for role {} and domain {}.", role, domainName);
            System.exit(1);
        }
    }

    public Map<String, String> getGlobalConfig() {
        return globalConfig;
    }

    public Set<String> getNodeNames() {
        return nodes.keySet();
    }

    public NodeConfig getNodeConfig(String nodeName) {
        return nodes.get(nodeName);
    }

    public List<RoleConfig> getDomainRoleConfig(NodeConfig nodeConfig) {
        List<RoleConfig> config = new ArrayList<>();
        for (DomainConfig domain : nodeConfig.getDomainConfigs()) {
            config.addAll(domain.getRoleConfigs());
        }
        return config;
    }

    public boolean isDatabasePerParticipant() {
        String databasePerParticipant = globalConfig.get(DATABASE_PER_PARTICIPANT);
        return (StringUtils.isEmpty(databasePerParticipant) || "true".equalsIgnoreCase(databasePerParticipant));
    }

    public String getKeystorePassword() {
        String password = globalConfig.get(KEYSTORE_PASSWORD);
        if (StringUtils.isEmpty(password)) {
            LOGGER.error("The password for the keystore is not configured in {} with key {}.",
                    ToolConfig.USEF_ENVIRONMENT_YAML, KEYSTORE_PASSWORD);
            System.exit(1);
        }
        return password;
    }

    public String getKeyStorePrivateKeyPassword() {
        String password = globalConfig.get(KEYSTORE_PRIVATE_KEY_PASSWORD);
        if (StringUtils.isEmpty(password)) {
            LOGGER.error("The private key for the keystore is not configured in {} with key {}.",
                    ToolConfig.USEF_ENVIRONMENT_YAML, KEYSTORE_PRIVATE_KEY_PASSWORD);
            System.exit(1);
        }
        return password;
    }

    /**
     * @return the timeServerConfig
     */
    public Map<String, Object> getTimeServerConfig() {
        return timeServerConfig;
    }

}
