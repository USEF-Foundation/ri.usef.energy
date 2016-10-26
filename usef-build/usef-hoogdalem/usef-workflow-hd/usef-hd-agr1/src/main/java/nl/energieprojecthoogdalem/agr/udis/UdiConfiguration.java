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

package nl.energieprojecthoogdalem.agr.udis;

import info.usef.agr.dto.device.capability.ProfileDto;
import info.usef.core.config.AbstractConfig;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * a class reading json files for the hoogdalem capabilities and endpoints uses {@link ObjectMapper}
 * */
@Singleton
public class UdiConfiguration
{
    private final Logger LOGGER = LoggerFactory.getLogger(UdiConfiguration.class);

    /**
     * returns a map of endpoints assigned to home ids
     * example of retrieving endpoint from an usef EAN:
     * ean.800000000000000032 -&#62; 32 -&#62; MZ29EBX0AP
     * @return Map with the key being the home id String and value the endpoint String
     * */
    public Map<String, String> getEndpoints()
    {
        Map<String, String> endpoints;

        ObjectMapper objectMapper = new ObjectMapper();
        String endpointsJSON = AbstractConfig.getConfigurationFolder() + "endpoints.json";
        TypeReference<HashMap<String,String>> typeRef = new TypeReference<HashMap<String, String>>() {};

        File endpointsFile = new File(endpointsJSON);
        try
        {
            endpoints = objectMapper.readValue(endpointsFile ,typeRef);
        }
        catch (IOException exception)
        {
            LOGGER.error("unable to get endpoints reason: ", exception.getMessage());
            endpoints = new HashMap<>();
        }

        return endpoints;
    }

    /**
     * returns a map of capabilities for hoogdalem
     * example of retrieving capability from an portfolio:
     * BATTTERY -&#62; capabilities: type ShiftCapability, id 42415454455259
     * @param  profileName the udi profile to search capabilities for
     * @return Map with profiles Strings as keys and values as {@link ProfileDto} capabilities
     * */
    public ProfileDto getCapabilities(String profileName)
    {
        ProfileDto capabilities = null;

        ObjectMapper objectMapper = new ObjectMapper();
        File endpointsFile = new File(AbstractConfig.getConfigurationFolder() + "capabilities.json");

        try
        {
            JsonNode root = objectMapper.readTree(endpointsFile);
            if(root.has(profileName))
                capabilities = objectMapper.treeToValue(root.get(profileName), ProfileDto.class);

            else
                LOGGER.warn("Profile {} is not known among our capability profiles!", profileName);

        }
        catch (IOException exception)
        {
            LOGGER.error("unable to get capabilities reason: ", exception.getMessage());
        }

        return capabilities;

    }

}
