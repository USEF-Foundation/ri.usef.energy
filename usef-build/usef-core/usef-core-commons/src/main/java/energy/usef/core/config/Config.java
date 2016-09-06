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

package energy.usef.core.config;

import energy.usef.core.util.DateTimeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Startup;
import javax.ejb.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property config class.
 *
 */
@Startup
@Singleton
public class Config extends AbstractConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    private final ScheduledExecutorService updateTimeExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Initialize a bean after the instance has been constructed.
     */
    @PostConstruct
    public void initBean() {
        try {
            readProperties();
            boolean usingServer = DateTimeUtil.updateSettings(getProperty(ConfigParam.TIME_SERVER),
                    getIntegerProperty(ConfigParam.TIME_SERVER_PORT));
            // if DateTimeUtil is using a server update the time at least every second.
            if (usingServer) {
                LOGGER.info("Running in TimeServer mode");
                updateTimeExecutor.scheduleAtFixedRate(DateTimeUtil::getCurrentDate, 1, 1, TimeUnit.SECONDS);
            } else {
                LOGGER.info("Running in system time mode");
            }
        } catch (IOException e) {
            LOGGER.error("Error while loading the properties: " + e.getMessage(), e);
        }
        startConfigWatcher();
    }

    /**
     * Clean up the bean before destroying this instance.
     */
    @PreDestroy
    public void cleanupBean() {
        updateTimeExecutor.shutdownNow();
        stopConfigWatcher();
    }

    /**
     * Gets a property value as a {@link String}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public String getProperty(ConfigParam configParam) {
        if (properties == null) {
            return null;
        }
        return properties.getProperty(configParam.name());
    }

    /**
     * Gets a property value as a {@link Double}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public Double getDoubleProperty(ConfigParam configParam) {
        return Double.parseDouble(properties.getProperty(configParam.name()));
    }

    /**
     * Gets a property value as an {@link Integer}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public Integer getIntegerProperty(ConfigParam configParam) {
        return Integer.parseInt(properties.getProperty(configParam.name()));
    }

    /**
     * Gets a property value as a {@link Boolean}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public Boolean getBooleanProperty(ConfigParam configParam) {
        return Boolean.parseBoolean(properties.getProperty(configParam.name()));
    }

    /**
     * Gets a property value as a {@link Long}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public Long getLongProperty(ConfigParam configParam) {
        return Long.parseLong(properties.getProperty(configParam.name()));
    }

    /**
     * Gets a property value as a {@link List} of {@link Integer}s.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public List<Integer> getIntegerPropertyList(ConfigParam configParam) {
        String[] stringArray = getStringPropertyArray(configParam.name());
        if (stringArray == null) {
            return new ArrayList<>();
        }
        List<Integer> list = new ArrayList<>(stringArray.length);
        for (String aStringArray : stringArray) {
            list.add(Integer.parseInt(aStringArray));
        }
        return list;
    }

    /**
     * Returns comma-separated property as an array string.
     *
     * @param key property key
     * @return properties as a string array
     */
    private String[] getStringPropertyArray(String key) {
        String str = properties.getProperty(key);
        if (str == null) {
            return new String[0];
        }
        return str.split(",");
    }

}
