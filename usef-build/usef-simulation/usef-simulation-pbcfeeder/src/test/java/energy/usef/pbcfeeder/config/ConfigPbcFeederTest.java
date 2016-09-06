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

package energy.usef.pbcfeeder.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import energy.usef.core.exception.TechnicalException;

import java.io.IOException;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit test to test the configuration of the AGR.
 */
public class ConfigPbcFeederTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigPbcFeederTest.class);

    private ConfigPbcFeeder configPbcFeeder;

    @Before
    public void init() throws Exception {
        configPbcFeeder = new ConfigPbcFeeder();
    }

    /**
     * Tests Config.findFile method.
     */
    @Test
    public void testFindFile() {
        try {
            configPbcFeeder.findFile("dummy-test.file", "");
        } catch (Exception e) {
            fail(e.getMessage());
        }

        try {
            configPbcFeeder.findFile("non-existent.file", "");
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
        assertTrue(!configPbcFeeder.getProperties().isEmpty());

        StringBuilder sb = new StringBuilder();
        for (Entry<Object, Object> property : configPbcFeeder.getProperties().entrySet()) {
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
        assertTrue(!configPbcFeeder.getProperties().isEmpty());
        for (ConfigPbcFeederParam param : ConfigPbcFeederParam.values()) {
            assertNotNull(param.name(), configPbcFeeder.getProperty(param));
            assertFalse(param.name(), configPbcFeeder.getProperty(param).isEmpty());
        }
    }

    @Test
    public void testWatcher() throws IOException {
        configPbcFeeder.initBean();

        assertTrue(configPbcFeeder.isConfigWatcherRunning());

        configPbcFeeder.cleanupBean();
    }

}
