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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.business.error.MessageFilterError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible to test the Message Filtering Service.
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Config.class })
public class MessageFilterServiceTest {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(MessageFilterServiceTest.class);

    private static final String ALLOWLISTED_DOMAIN = "allowlisted.com";

    private static final String DENYLISTED_IP = "192.168.1.10";
    private static final String DENYLISTED_DOMAIN = "denylisted.test.usef.energy";

    private static final String NON_DENYLISTED_IP = "172.232.99.1";
    private static final String NON_DENYLISTED_DOMAIN = "freeaccess.com";

    private static final String ALLOW_LIST_FILENAME = "transport-allowlist.yaml";
    private static final String DENY_LIST_FILENAME = "transport-denylist.yaml";

    @Mock
    private Config config;

    private MessageFilterService messageFilterService = new MessageFilterService();

    @Before
    public void init() throws Exception {
        Whitebox.setInternalState(messageFilterService, "config", config);
        PowerMockito.when(config.getProperty(ConfigParam.SENDER_ALLOW_LIST_FILENAME)).thenReturn(ALLOW_LIST_FILENAME);
        PowerMockito.when(config.getProperty(ConfigParam.SENDER_DENY_LIST_FILENAME)).thenReturn(DENY_LIST_FILENAME);
        PowerMockito.when(config.findFile(Matchers.any(), Matchers.any())).thenCallRealMethod();
    }

    /**
     * Tests if a message with an effectively denylisted ip is correctly rejected by the {@link MessageFilterService}. Test will
     * fail if no {@link BusinessException} is thrown.
     *
     * @throws BusinessException
     */
    @Test
    public void testFilterMessageWithDenylistedIP() {

        PowerMockito.when(config.getBooleanProperty(ConfigParam.SENDER_ALLOW_LIST_FORCED)).thenReturn(false);
        try {
            messageFilterService.filterMessage(NON_DENYLISTED_DOMAIN, DENYLISTED_IP);

            fail("Exception should have been thrown since the address is denylisted.");
        } catch (BusinessException e) {
            LOGGER.info("Exception thrown: {}", e.getBusinessError().getError());
            assertEquals(e.getBusinessError(), MessageFilterError.ADDRESS_IS_DENYLISTED);
        }
    }

    /**
     * Tests if a message with an effectively denylisted domain is correctly rejected by the {@link MessageFilterService}. Test will
     * fail if no {@link BusinessException} is thrown.
     *
     * @throws BusinessException
     */
    @Test
    public void testFilterMessageWithDenylistedDomain() {
        PowerMockito.when(config.getBooleanProperty(ConfigParam.SENDER_ALLOW_LIST_FORCED)).thenReturn(false);
        try {
            messageFilterService.filterMessage(DENYLISTED_DOMAIN, NON_DENYLISTED_IP);

            fail("Exception should have been thrown since the address is denylisted.");
        } catch (BusinessException e) {
            LOGGER.info("Exception thrown: {}", e.getBusinessError().getError());
            assertEquals(e.getBusinessError(), MessageFilterError.ADDRESS_IS_DENYLISTED);
        }
    }

    /**
     * Checks when all values are correct no exception will be thrown.
     *
     * @throws BusinessException
     */
    @Test
    public void testFilterMessageValid() {
        PowerMockito.when(config.getBooleanProperty(ConfigParam.SENDER_ALLOW_LIST_FORCED)).thenReturn(false);
        try {
            messageFilterService.filterMessage(NON_DENYLISTED_DOMAIN, NON_DENYLISTED_IP);
        } catch (BusinessException e) {
            fail("Exception should not have been thrown since the address is not denylisted.");
        }
    }

    /**
     * Check if allowlisting is turned on non denylisted values will get rejected.
     */
    @Test
    public void testFilterMessageAllowlisted() {
        PowerMockito.when(config.getBooleanProperty(ConfigParam.SENDER_ALLOW_LIST_FORCED)).thenReturn(true);
        try {
            messageFilterService.filterMessage(NON_DENYLISTED_DOMAIN, NON_DENYLISTED_IP);

            fail("Exception should have been thrown since the address is not allowlisted.");
        } catch (BusinessException e) {
            LOGGER.info("Exception thrown: {}", e.getBusinessError().getError());
            assertEquals(e.getBusinessError(), MessageFilterError.PARTICIPANT_NOT_ALLOWLISTED);
        }
    }

    /**
     * Test that allowlisting is not done based on sender host ip.
     */
    @Test
    public void testFilterMessageAllowlistedNotByIP() {
        PowerMockito.when(config.getBooleanProperty(ConfigParam.SENDER_ALLOW_LIST_FORCED)).thenReturn(true);
        try {
            messageFilterService.filterMessage(NON_DENYLISTED_DOMAIN, ALLOWLISTED_DOMAIN);

            fail("Exception should have been thrown since your not allowed to allowlist on sender ip.");
        } catch (BusinessException e) {
            LOGGER.info("Exception thrown: {}", e.getBusinessError().getError());
            assertEquals(e.getBusinessError(), MessageFilterError.PARTICIPANT_NOT_ALLOWLISTED);
        }
    }

    /**
     * Test that allowlisting is accepted when allowlisted domain is used.
     */
    @Test
    public void testFilterMessageAllowlistedValid() {
        PowerMockito.when(config.getBooleanProperty(ConfigParam.SENDER_ALLOW_LIST_FORCED)).thenReturn(true);
        try {
            messageFilterService.filterMessage(ALLOWLISTED_DOMAIN, NON_DENYLISTED_IP);
        } catch (BusinessException e) {
            fail("Exception should not have been thrown, domain is allowlisted.");
        }
    }

    /**
     * Test that allowlisting is accepted when allowlisted domain is used.
     */
    @Test
    public void testFilterMessageAllowlistedButIPDenylisted() {
        PowerMockito.when(config.getBooleanProperty(ConfigParam.SENDER_ALLOW_LIST_FORCED)).thenReturn(true);
        try {
            messageFilterService.filterMessage(ALLOWLISTED_DOMAIN, DENYLISTED_IP + "," + NON_DENYLISTED_IP);

            fail("Exception should have been thrown since your not allowed to allowlist on sender ip.");
        } catch (BusinessException e) {
            LOGGER.info("Exception thrown: {}", e.getBusinessError().getError());
            assertEquals(e.getBusinessError(), MessageFilterError.ADDRESS_IS_DENYLISTED);
        }
    }

    /**
     * Test that allowlisting is accepted when allowlisted domain is used.
     */
    @Test
    public void testFilterMessageAllowlistedButIPDenylistedLast() {
        PowerMockito.when(config.getBooleanProperty(ConfigParam.SENDER_ALLOW_LIST_FORCED)).thenReturn(true);
        try {
            messageFilterService.filterMessage(ALLOWLISTED_DOMAIN, NON_DENYLISTED_IP + "," + DENYLISTED_IP);

            fail("Exception should have been thrown since your not allowed to allowlist on sender ip.");
        } catch (BusinessException e) {
            LOGGER.info("Exception thrown: {}, {}", e.getBusinessError().getError());
            assertEquals(e.getBusinessError(), MessageFilterError.ADDRESS_IS_DENYLISTED);
        }
    }

}
