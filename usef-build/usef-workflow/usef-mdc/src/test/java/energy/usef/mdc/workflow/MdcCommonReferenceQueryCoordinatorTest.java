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

package energy.usef.mdc.workflow;

import static java.util.stream.Collectors.toList;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.XMLUtil;
import energy.usef.mdc.service.business.MdcCoreBusinessService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link MdcCommonReferenceQueryCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class MdcCommonReferenceQueryCoordinatorTest {

    private MdcCommonReferenceQueryCoordinator coordinator;

    @Mock
    private Config config;
    @Mock
    private MdcCoreBusinessService mdcCoreBusinessService;
    @Mock
    private JMSHelperService jmsHelperService;

    @Before
    public void setUp() throws Exception {
        coordinator = new MdcCommonReferenceQueryCoordinator();
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, mdcCoreBusinessService);
    }

    @Test
    public void testHandleEvent() throws Exception {
        // stubbing of the config
        PowerMockito.when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("mdc.usef-example.com");
        // stubbing of the MdcCoreBusinessService
        PowerMockito.when(mdcCoreBusinessService.findAllConnectionEntityAddresses())
                .then(invocation -> IntStream.rangeClosed(1, 5).mapToObj(i -> "ean.0000" + i).collect(toList()));
        PowerMockito.when(mdcCoreBusinessService.findAllCommonReferenceOperatorDomains())
                .thenReturn(Arrays.asList("cro1.usef-example.com", "cro2.usef-example.com"));

        coordinator.handleEvent(new CommonReferenceQueryEvent());

        // verifications
        Mockito.verify(mdcCoreBusinessService, Mockito.times(1)).findAllCommonReferenceOperatorDomains();
        Mockito.verify(mdcCoreBusinessService, Mockito.times(1)).findAllConnectionEntityAddresses();
        ArgumentCaptor<String> xmlMessageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(2)).sendMessageToOutQueue(xmlMessageCaptor.capture());

        List<String> sentMessages = xmlMessageCaptor.getAllValues();
        sentMessages.stream().map(xml -> XMLUtil.xmlToMessage(xml, CommonReferenceQuery.class)).forEach(commonReferenceQuery -> {
            Assert.assertEquals(5, commonReferenceQuery.getConnectionEntityAddress().size());
            Assert.assertNotNull(commonReferenceQuery.getMessageMetadata().getRecipientDomain());
            Assert.assertEquals(CommonReferenceEntityType.AGGREGATOR, commonReferenceQuery.getEntity());
            Assert.assertNull(commonReferenceQuery.getEntityAddress());
        });
    }
}
