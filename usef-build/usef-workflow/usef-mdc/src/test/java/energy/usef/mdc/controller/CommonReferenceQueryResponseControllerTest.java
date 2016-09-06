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

package energy.usef.mdc.controller;

import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.mdc.service.business.MdcCoreBusinessService;
import energy.usef.mdc.model.CommonReferenceQueryState;
import energy.usef.mdc.model.CommonReferenceQueryStatus;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link CommonReferenceQueryResponseController} class.
 */
@RunWith(PowerMockRunner.class)
public class CommonReferenceQueryResponseControllerTest {

    private CommonReferenceQueryResponseController controller;

    @Mock
    private MdcCoreBusinessService mdcCoreBusinessService;

    @Before
    public void setUp() {
        controller = new CommonReferenceQueryResponseController();
        Whitebox.setInternalState(controller, mdcCoreBusinessService);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testActionOnSuccessfulResult() throws Exception {
        controller.action(buildMessage(DispositionSuccessFailure.SUCCESS), null);
        ArgumentCaptor<CommonReferenceQueryState> stateCaptor = ArgumentCaptor.forClass(CommonReferenceQueryState.class);
        Mockito.verify(mdcCoreBusinessService, Mockito.times(1))
                .storeConnectionsForCommonReferenceOperator(Matchers.any(List.class), Matchers.eq("cro.usef-example.com"));
        Mockito.verify(mdcCoreBusinessService, Mockito.times(1)).storeCommonReferenceQueryState(stateCaptor.capture());
        CommonReferenceQueryState state = stateCaptor.getValue();
        Assert.assertNotNull(state);
        Assert.assertEquals(CommonReferenceQueryStatus.SUCCESS, state.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testActionOnFailureResult() throws Exception {
        controller.action(buildMessage(DispositionSuccessFailure.FAILURE), null);
        ArgumentCaptor<CommonReferenceQueryState> stateCaptor = ArgumentCaptor.forClass(CommonReferenceQueryState.class);
        Mockito.verify(mdcCoreBusinessService, Mockito.times(0))
                .storeConnectionsForCommonReferenceOperator(Matchers.any(List.class), Matchers.eq("cro.usef-example.com"));
        Mockito.verify(mdcCoreBusinessService, Mockito.times(1)).storeCommonReferenceQueryState(stateCaptor.capture());
        CommonReferenceQueryState state = stateCaptor.getValue();
        Assert.assertNotNull(state);
        Assert.assertEquals(CommonReferenceQueryStatus.FAILURE, state.getStatus());
    }

    private CommonReferenceQueryResponse buildMessage(DispositionSuccessFailure result) {
        CommonReferenceQueryResponse response = new CommonReferenceQueryResponse();
        response.setMessageMetadata(new MessageMetadataBuilder().conversationID()
                .messageID()
                .timeStamp()
                .senderDomain("cro.usef-example.com")
                .senderRole(USEFRole.CRO)
                .recipientDomain("mdc.usef-example.com")
                .recipientRole(USEFRole.MDC)
                .build());
        response.setResult(result);
        return response;
    }
}
