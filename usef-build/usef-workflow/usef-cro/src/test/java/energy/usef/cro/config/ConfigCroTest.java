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

package energy.usef.cro.config;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import energy.usef.core.exception.TechnicalException;

import java.io.IOException;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit test to test the configuration of the CRO.
 */
public class ConfigCroTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigCroTest.class);

    private ConfigCro configCro;

    @Before
    public void init() throws Exception {
        configCro = new ConfigCro();
    }

    /**
     * Tests Config.findFile method.
     */
    @Test
    public void findFileTest() {
        try {
            configCro.findFile("dummy-test.file", "");
        } catch (Exception e) {
            fail(e.getMessage());
        }

        try {
            configCro.findFile("non-existent.file", "");
            fail("TechnicalException should have been thrown");
        } catch (Exception e) {
            assertTrue(e instanceof TechnicalException);
        }
    }

    /**
     * Tests all properties which are defined in the configuration.
     * 
     * @throws IOException
     */
    @Test
    public void loadDefaultPropertiesTest() throws IOException {
        assertTrue(!configCro.getProperties().isEmpty());

        StringBuilder sb = new StringBuilder();
        for (Entry<Object, Object> property : configCro.getProperties().entrySet()) {
            sb.append(property.getKey()).append(" = ").append(property.getValue()).append("\n");
        }
        LOGGER.debug(sb.toString());
    }

    /**
     * Tests the getProperty method on all Config parameters.
     * 
     * @throws IOException
     */
    @Test
    public void checkAllParametersTest() throws IOException {
        assertTrue(!configCro.getProperties().isEmpty());
        for (ConfigCroParam param : ConfigCroParam.values()) {
            assertTrue(param.name(), !configCro.getProperty(param).isEmpty());
            Assert.assertFalse(param.name(), configCro.getProperty(param).isEmpty());
        }
    }

    /**
     * Tests the getIntegerProperty method on Config parameters which have PropertyClass Integer and checks also if they have a
     * value.
     * 
     * @throws IOException
     */
    @Test
    public void checkIntegerParametersTest() throws IOException {
        assertTrue(!configCro.getProperties().isEmpty());
        for (ConfigCroParam param : ConfigCroParam.values()) {
            if (Integer.class == param.getPropertyClass()) {
                assertTrue(configCro.getIntegerProperty(param) != 0);
            }
        }
    }

    @Test
    public void watcherTest() throws IOException {
        configCro.initBean();

        // assertTrue(configCro.isConfigWatcherRunning());

        configCro.cleanupBean();
    }

}
