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

package energy.usef.dso.config;

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
public class ConfigDsoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigDsoTest.class);

    private ConfigDso configDso;

    @Before
    public void init() throws Exception {
        configDso = new ConfigDso();
    }

    /**
     * Tests Config.findFile method.
     */
    @Test
    public void testFindFile() {
        try {
            configDso.findFile("dummy-test.file", "");
        } catch (Exception e) {
            fail(e.getMessage());
        }

        try {
            configDso.findFile("non-existent.file", "");
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
    public void testLoadDefaultProperties() throws IOException {
        assertTrue(!configDso.getProperties().isEmpty());

        StringBuilder sb = new StringBuilder();
        for (Entry<Object, Object> property : configDso.getProperties().entrySet()) {
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
    public void testCheckAllParameters() throws IOException {
        assertTrue(!configDso.getProperties().isEmpty());
        for (ConfigDsoParam param : ConfigDsoParam.values()) {
            Assert.assertNotNull(param.name(), configDso.getProperty(param));
            assertTrue(param.name(), !configDso.getProperty(param).isEmpty());
        }
    }

    /**
     * Tests the getIntegerProperty method on Config parameters which have PropertyClass Integer and checks also if they have a
     * value.
     * 
     * @throws IOException
     */
    @Test
    public void testCheckIntegerParameters() throws IOException {
        assertTrue(!configDso.getProperties().isEmpty());
        for (ConfigDsoParam param : ConfigDsoParam.values()) {
            if (Integer.class == param.getPropertyClass()) {
                assertTrue(param.name(), configDso.getIntegerProperty(param) != 0);
            }
        }
    }

    @Test
    public void testWatcher() throws IOException {
        configDso.initBean();

        assertTrue(configDso.isConfigWatcherRunning());

        configDso.cleanupBean();
    }

}
