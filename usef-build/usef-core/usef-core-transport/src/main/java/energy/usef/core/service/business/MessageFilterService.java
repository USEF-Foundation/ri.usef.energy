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

package energy.usef.core.service.business;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.business.error.MessageFilterError;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Service class in charge of filtering incoming HTTP messages and, based on the denylisted addresses (IP or EA), accept or reject
 * them.
 */
@Singleton
public class MessageFilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageFilterService.class);

    private static final String ALLOW_LIST_FILE_NAME = "transport-allowlist.yaml";
    private static final String DENY_LIST_FILE_NAME = "transport-denylist.yaml";

    @Inject
    private Config config;

    /**
     * Filter an incoming message. The message will be rejected if the sender's address is in the deny list.
     *
     * @param senderDomain - The domain of the received message
     * @param hostlist - A string containing hostnames/IP addresses of the sender, separated by ','
     * @throws BusinessException
     */
    public void filterMessage(String senderDomain, String hostlist) throws BusinessException {
        // if sender domain not allowlisted throw exceptions, otherwise check denylist

        if (config.getBooleanProperty(ConfigParam.SENDER_ALLOW_LIST_FORCED)) {
            List<String> allowlist = loadYamlList(Config.getConfigurationFolder() +
                    config.getProperty(ConfigParam.SENDER_ALLOW_LIST_FILENAME), ALLOW_LIST_FILE_NAME);
            if (!allowlist.contains(senderDomain)) {
                throw new BusinessException(MessageFilterError.PARTICIPANT_NOT_ALLOWLISTED);
            }
        }

        List<String> denyList = loadYamlList(Config.getConfigurationFolder() +
                config.getProperty(ConfigParam.SENDER_DENY_LIST_FILENAME), DENY_LIST_FILE_NAME);
        if (!denyList.isEmpty()) {
            if (denyList.contains(senderDomain)) {
                LOGGER.info("The sender of the message is denylisted.");
                throw new BusinessException(MessageFilterError.ADDRESS_IS_DENYLISTED);
            }
            for (String host : hostlist.split(",")) {
                if (denyList.contains(host)) {
                    LOGGER.info("The sender of the message is denylisted.");
                    throw new BusinessException(MessageFilterError.ADDRESS_IS_DENYLISTED);

                }
            }
        }
    }

    /**
     * Loads a Yaml file with a list of strings.
     *
     * @param filename
     * @param filenameOfTheList
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<String> loadYamlList(String filename, String filenameOfTheList) {
        Object result = null;
        try {
            InputStream is = config.findFile(filename, filenameOfTheList);

            result = new Yaml().load(is);
        } catch (Exception e) {
            LOGGER.warn("Cannot load {} from {}: {}", filename, filenameOfTheList, e.getMessage(), e);
        }

        if (result instanceof List) {
            return (List<String>) result;
        } else if (result != null) {
            LOGGER.warn("No valid list found in {}", filename);
        }
        return new ArrayList<>();
    }

}
