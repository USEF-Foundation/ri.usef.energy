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

package energy.usef.agr.workflow.validate.flexoffer;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.service.helper.JMSHelperService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class in charge of the unit tests related to the {@link AgrFlexOfferRevocationCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class AgrFlexOfferRevocationCoordinatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrFlexOfferRevocationCoordinatorTest.class);
    private static final long FLEX_OFFER_SEQUENCE = 20141215140400000l;
    private static final String PARTICIPANT_DOMAIN = "usef-example.com";

    private AgrFlexOfferRevocationCoordinator coordinator;

    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private Config config;

    @Before
    public void init() {
        coordinator = new AgrFlexOfferRevocationCoordinator();
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, config);

        PowerMockito.when(config.getProperty(Matchers.eq(ConfigParam.HOST_DOMAIN))).thenReturn("agr.usef-example.com");
    }

    @Test
    public void testInvokeWorkflowDSOSucceeds() {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        coordinator.handleEvent(new FlexOfferRevocationEvent(FLEX_OFFER_SEQUENCE, PARTICIPANT_DOMAIN, USEFRole.DSO));

        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(messageCaptor.capture());

        String flexOfferRevocationXml = messageCaptor.getValue();
        LOGGER.info("Flex Offer revocation message:\n{}", flexOfferRevocationXml);
        Assert.assertNotNull(flexOfferRevocationXml);
    }

    @Test
    public void testInvokeWorkflowBRPSucceeds() {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        coordinator.handleEvent(new FlexOfferRevocationEvent(FLEX_OFFER_SEQUENCE, PARTICIPANT_DOMAIN, USEFRole.BRP));

        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(messageCaptor.capture());

        String flexOfferRevocationXml = messageCaptor.getValue();
        LOGGER.info("Flex Offer revocation message:\n{}", flexOfferRevocationXml);
        Assert.assertNotNull(flexOfferRevocationXml);
    }

}
