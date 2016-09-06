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

package energy.usef.cro.controller;

import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdate;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.cro.service.business.CommonReferenceUpdateBusinessService;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class for CommonReferenceUpdateController.
 *
 */
@RunWith(PowerMockRunner.class)
public class CommonReferenceUpdateControllerTest {

    @Mock
    private CommonReferenceUpdateBusinessService commonReferenceUpdateBusinessService;

    @Mock
    private JMSHelperService jmsHelperService;

    private CommonReferenceUpdateController controller = new CommonReferenceUpdateController();

    @Before
    public void init() {
        Whitebox.setInternalState(controller,
                "commonReferenceUpdateBusinessService",
                commonReferenceUpdateBusinessService);
        Whitebox.setInternalState(controller,
                "jmsService", jmsHelperService);
    }

    /**
     * Tests CommonReferenceUpdate.action method.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCommonReferenceUpdateFromDSO() {
        CommonReferenceUpdate commonReferenceUpdate = new CommonReferenceUpdate();
        commonReferenceUpdate.setEntityAddress("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6");
        commonReferenceUpdate.setEntity(CommonReferenceEntityType.CONGESTION_POINT);

        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setMessageID("testId");
        messageMetadata.setSenderDomain("test.sender.domain");
        commonReferenceUpdate.setMessageMetadata(messageMetadata);

        try {
            controller.action(commonReferenceUpdate, null);
        } catch (BusinessException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.verify(commonReferenceUpdateBusinessService, Mockito.times(1))
                .updateCongestionPoints(Matchers.any(CommonReferenceUpdate.class), Matchers.any(List.class));

        Mockito.verify(jmsHelperService, Mockito.times(1))
                .sendMessageToOutQueue(Matchers.anyString());
    }

    /**
     * Tests CommonReferenceUpdate.action method.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCommonReferenceUpdateFromAGR() {
        CommonReferenceUpdate commonReferenceUpdate = new CommonReferenceUpdate();
        commonReferenceUpdate.setEntityAddress("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6");
        commonReferenceUpdate.setEntity(CommonReferenceEntityType.AGGREGATOR);

        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setMessageID("testId");
        messageMetadata.setSenderDomain("test.sender.domain");
        commonReferenceUpdate.setMessageMetadata(messageMetadata);

        try {
            controller.action(commonReferenceUpdate, null);
        } catch (BusinessException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.verify(commonReferenceUpdateBusinessService, Mockito.times(1))
                .updateAggregatorConnections(Matchers.any(CommonReferenceUpdate.class), Matchers.any(List.class));

        Mockito.verify(jmsHelperService, Mockito.times(1))
                .sendMessageToOutQueue(Matchers.anyString());
    }

    /**
     * Tests CommonReferenceUpdate.action method.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCommonReferenceUpdateFromBRP() {
        CommonReferenceUpdate commonReferenceUpdate = new CommonReferenceUpdate();
        commonReferenceUpdate.setEntityAddress("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6");
        commonReferenceUpdate.setEntity(CommonReferenceEntityType.BRP);

        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setMessageID("testId");
        messageMetadata.setSenderDomain("test.sender.domain");
        commonReferenceUpdate.setMessageMetadata(messageMetadata);

        try {
            controller.action(commonReferenceUpdate, null);
        } catch (BusinessException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.verify(commonReferenceUpdateBusinessService, Mockito.times(1))
                .updateBalanceResponsiblePartyConnections(Matchers.any(CommonReferenceUpdate.class), Matchers.any(List.class));

        Mockito.verify(jmsHelperService, Mockito.times(1))
                .sendMessageToOutQueue(Matchers.anyString());
    }

}
