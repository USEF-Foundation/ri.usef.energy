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

package energy.usef.agr.repository;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.dto.device.capability.ProfileDto;
import energy.usef.agr.model.ConnectionForecastSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository class in charge of the 'database' operations related to the {@link ConnectionForecastSnapshot} entities. The
 * serialization format behind this repository might not be a database (can be JSON file).
 */
@Stateless
public class CapabilityProfileRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilityProfileRepository.class);
    private static final String CAPABILITY_FILE = "capabilities.json";
    @Inject
    private ConfigAgr configAgr;

    /**
     * Method to writeProfiles to JSON data.
     *
     * @param profiles
     * @return
     */
    public String writeProfiles(Map<String, ProfileDto> profiles) {
        try {
            return new ObjectMapper().writeValueAsString(profiles);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Method to read json data to {@link ProfileDto}'s.
     *
     * @param data
     * @return
     */
    public Map<String, ProfileDto> readProfiles(String data) {
        try {
            TypeReference<Map<String, ProfileDto>> valueTypeRef = new TypeReference<Map<String, ProfileDto>>() {
            };
            return new ObjectMapper().readValue(data, valueTypeRef);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return new HashMap<>();
    }

    /**
     * Read the capabilities from the capabilites.json file found in {@link ConfigAgr#getConfigurationFolder}.
     *
     * @return the map with the profiles
     */
    public Map<String, ProfileDto> readFromConfigFile() {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(ConfigAgr.getConfigurationFolder() + CAPABILITY_FILE));
            return readProfiles(new String(encoded));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return new HashMap<>();
    }

}
