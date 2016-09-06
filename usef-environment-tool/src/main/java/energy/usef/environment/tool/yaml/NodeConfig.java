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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides the configuration of nodes within a usef environment.
 */
public class NodeConfig {
    private String name;
    private String address;
    private String basePath;
    private String pathSep;
    private Map<String, DomainConfig> domainConfigs = new HashMap<String, DomainConfig>();

    /**
     * Returns a DomainConfig for a specific domain in the node.
     * @param domain the domain to return the DomainConfig for
     * @return the DomainConfig of the specified domain
     */
    public DomainConfig getRoleConfig(String domain) {
        return domainConfigs.get(domain);
    }

    /**
     * Adds a DomainConfig to this node.
     * @param domainConfig the DomainConfig to add
     */
    public void add(DomainConfig domainConfig) {
        domainConfigs.put(domainConfig.getDomain(), domainConfig);
    }

    /**
     * 
     * @return the domains configured for this node
     */
    public Set<String> getDomains() {
        return domainConfigs.keySet();
    }

    /**
     * 
     * @return the DomainConfigs for this node
     */
    public Collection<DomainConfig> getDomainConfigs() {
        return domainConfigs.values();
    }

    /**
     * Constructs a NodeConfig for a node.
     * @param name the name of the node
     * @param address the address of the node
     * @param basePath the basePath of the node
     * @param pathSep the path separator for the node
     */
    public NodeConfig(String name, String address, String basePath, String pathSep) {
        super();
        this.name = name;
        this.address = address;
        this.basePath = basePath;
        this.pathSep = pathSep;
    }

    /**
     * @return the basePath
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * @return the pathSep
     */
    public String getPathSep() {
        return pathSep;
    }

    /**
     * 
     * @return the folder which is used by WildFly (locally defined from the node's point of view)
     */
    public String getWildFlyFolder() {
        return String.join(pathSep, new String[] { basePath, "usef-environment", "nodes", name });
    }
    
    /**
     * 
     * @param subFolder the subfolder within the wildfly folder
     * @return the absolute location of the subfolder (locally defined from the node's point of view)
     */
    public String getWildFlySubFolder(String subFolder) {
        return String.join(pathSep, new String[] { getWildFlyFolder(), subFolder });
    }

    /**
     * 
     * @return the Database URL, defined from the node's point of view
     */
    public String getDbUrl() {
        String filename = getWildFlySubFolder("data") + pathSep + "usef_db";
        String url = "jdbc:h2:tcp://" + address + "/" + filename.replace(File.separatorChar, '/').replace("//", "/") + ";CIPHER=AES;MVCC=true";
        return url;
    }


    /**
     * 
     * @param password the password for the encrypted database
     * @return the Database URL, including username and password and defined from the node's point of view
     */
    public String getDbUrlIncludingUsernameAndPassword(String password) {
        String url = getDbUrl() + ";USER=" + ToolConfig.USER + ";PASSWORD=" + password;
        return url;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the node
     */
    public String getNode() {
        return name;
    }

    /**
     * Returns the DomainConfig for a specific domain.
     * @param domainName the name of the domain to return the DomainConfig for
     * @return the DomainConfig for domain with name domainName
     */
    public DomainConfig getDomain(String domainName) {
        return domainConfigs.get(domainName);
    }

    /**
     * 
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        NodeConfig other = (NodeConfig) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "NodeConfig [node=" + name + "]";
    }


}
