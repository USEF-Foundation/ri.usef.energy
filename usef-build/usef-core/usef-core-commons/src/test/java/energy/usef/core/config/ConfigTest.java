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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.util.DateTimeUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit test for the common USEF configuration items.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DateTimeUtil.class })
public class ConfigTest {
    /**
     * 
     */
    private static final String TESTPROPERTY = "TESTPROPERTY";
    private static final String TESTPROPERTY_VALUE = "TESTING";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigTest.class);

    private Config config;

    @Before
    public void init() throws Exception {
        PowerMockito.mockStatic(DateTimeUtil.class);
        config = new Config();
        config.initBean();
    }

    /**
     * Tests Config.findFile method.
     */
    @Test
    public void findFileTest() {
        try {
            config.findFile("config-local.properties", "");
        } catch (Exception e) {
            fail(e.getMessage());
        }

        try {
            config.findFile("non-existent.file", "");
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
        config.properties = config.readDefaultProperties();
        assertTrue(!config.properties.isEmpty());
        assertTrue(!config.properties.containsKey(TESTPROPERTY));
        StringBuilder sb = new StringBuilder();
        for (Entry<Object, Object> property : config.properties.entrySet()) {
            sb.append(property.getKey()).append(" = ").append(property.getValue()).append("\n");
        }
        LOGGER.debug(sb.toString());
    }

    /**
     * Tests all properties which are defined in the configuration.
     * 
     * @throws IOException
     */
    @Test
    public void loadLocalPropertiesTest() throws IOException {
        assertTrue(!config.properties.isEmpty());

        assertTrue(config.properties.containsKey(TESTPROPERTY));
        assertTrue(TESTPROPERTY_VALUE.equals(config.properties.get(TESTPROPERTY)));
    }

    /**
     * Tests the getProperty method on all Config parameters.
     * 
     * @throws IOException
     */
    @Test
    public void checkAllParametersTest() throws IOException {
        assertTrue(!config.properties.isEmpty());
        for (ConfigParam param : ConfigParam.values()) {
            assertTrue(param.name() + " has no default value in the config.properties file", config.getProperty(param) != null);
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
        assertTrue(!config.properties.isEmpty());
        for (ConfigParam param : ConfigParam.values()) {
            if (Integer.class == param.getPropertyClass()) {
                assertTrue(config.getIntegerProperty(param) != 0);
            }
        }
    }

    /**
     * Tests the getDoubleProperty method on Config parameters which have PropertyClass Double and checks also if they have a value.
     * 
     * @throws IOException
     */
    @Test
    public void checkDoubleParametersTest() throws IOException {
        assertTrue(!config.properties.isEmpty());
        for (ConfigParam param : ConfigParam.values()) {
            if (Double.class == param.getPropertyClass()) {
                assertTrue(config.getDoubleProperty(param) != 0);
            }
        }
    }

    /**
     * Tests the getBooleanProperty method on Config parameters which have PropertyClass Boolean and checks also if they have a
     * value.
     * 
     * @throws IOException
     */
    @Test
    public void checkBooleanParametersTest() throws IOException {
        assertTrue(!config.properties.isEmpty());
        for (ConfigParam param : ConfigParam.values()) {
            if (Boolean.class == param.getPropertyClass()) {
                assertTrue(config.getBooleanProperty(param) || !config.getBooleanProperty(param));
            }
        }
    }

    /**
     * Tests the getLongProperty method on Config parameters which have PropertyClass Long and checks also if they have a value.
     * 
     * @throws IOException
     */
    @Test
    public void checkLongParametersTest() throws IOException {
        assertTrue(!config.properties.isEmpty());
        for (ConfigParam param : ConfigParam.values()) {
            if (Long.class == param.getPropertyClass()) {
                assertTrue(config.getLongProperty(param) != 0);
            }
        }
    }

    /**
     * Test property RETRY_HTTP_ERROR_CODES, also to cover getIntegerPropertyList().
     * 
     * @throws IOException
     */
    @Test
    public void retryHttpErrorCodesTest() throws IOException {
        List<Integer> errorCodes = config.getIntegerPropertyList(ConfigParam.RETRY_HTTP_ERROR_CODES);

        assertEquals(4, errorCodes.size());
        for (Integer errorCode : errorCodes) {
            assertTrue(errorCode > 0);
        }
    }

    @Test
    public void watcherTest() throws IOException {
        config.initBean();

        assertTrue(config.isConfigWatcherRunning());

        config.cleanupBean();
    }
}
