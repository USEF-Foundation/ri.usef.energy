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

package energy.usef.agr.workflow.plan.commonreferenceupdate;

import energy.usef.agr.model.CommonReferenceOperator;
import energy.usef.agr.model.SynchronisationConnection;
import energy.usef.agr.model.SynchronisationConnectionStatus;
import energy.usef.agr.model.SynchronisationConnectionStatusType;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdate;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.XMLUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class AgrCommonReferenceUpdateCoordinatorTest {

    private static final String CONNECTION_PREFIX = "ean.00000000000";
    private AgrCommonReferenceUpdateCoordinator agrCommonReferenceUpdateCoordinator;
    @Mock
    private AgrPlanboardBusinessService businessService;
    @Mock
    private Config config;
    @Mock
    private JMSHelperService jmsHelperService;

    @Before
    public void setUp() throws Exception {
        agrCommonReferenceUpdateCoordinator = new AgrCommonReferenceUpdateCoordinator();
        Whitebox.setInternalState(agrCommonReferenceUpdateCoordinator, config);
        Whitebox.setInternalState(agrCommonReferenceUpdateCoordinator, jmsHelperService);
        Whitebox.setInternalState(agrCommonReferenceUpdateCoordinator, businessService);
    }

    @Test
    public void testHandleEvent() throws Exception {
        Mockito.when(businessService.findConnectionsPerCRO()).thenReturn(buildSynchronisationConnectionsPerCro());
        Mockito.when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("agr1.usef-example.com");
        Mockito.when(config.getProperty(ConfigParam.HOST_ROLE)).thenReturn("AGR");
        // invocation
        agrCommonReferenceUpdateCoordinator.handleEvent(new CommonReferenceUpdateEvent());
        // validation
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(messageCaptor.capture());
        String message = messageCaptor.getValue();
        Assert.assertNotNull(message);
        CommonReferenceUpdate commonReferenceUpdate = transformMessageToJava(message);
        Assert.assertEquals(2, commonReferenceUpdate.getConnection().size());
        Assert.assertEquals("cro1.usef-example.com", commonReferenceUpdate.getMessageMetadata().getRecipientDomain());
        Assert.assertEquals(USEFRole.CRO, commonReferenceUpdate.getMessageMetadata().getRecipientRole());
        Assert.assertEquals("agr1.usef-example.com", commonReferenceUpdate.getMessageMetadata().getSenderDomain());
        Assert.assertEquals(USEFRole.AGR, commonReferenceUpdate.getMessageMetadata().getSenderRole());
        Assert.assertEquals(CommonReferenceEntityType.AGGREGATOR, commonReferenceUpdate.getEntity());

    }

    private CommonReferenceUpdate transformMessageToJava(String message) {
        return XMLUtil.xmlToMessage(message, CommonReferenceUpdate.class);
    }

    private Map<String, List<SynchronisationConnection>> buildSynchronisationConnectionsPerCro() {
        Map<String, List<SynchronisationConnection>> result = new HashMap<>();
        String croDomain1 = "cro1.usef-example.com";
        String croDomain2 = "cro2.usef-example.com";
        result.put(croDomain1, buildSynchronsisationConnections(2, croDomain1));
        result.put(croDomain2, new ArrayList<>());
        return result;
    }

    private List<SynchronisationConnection> buildSynchronsisationConnections(int amount, String croDomain) {
        return IntStream.rangeClosed(1, amount).mapToObj(index -> {
            SynchronisationConnection synchronisationConnection = new SynchronisationConnection();
            synchronisationConnection.setEntityAddress(CONNECTION_PREFIX + amount);
            synchronisationConnection.getStatusses().add(buildSynchronisationConnectionStatus(croDomain));
            return synchronisationConnection;
        }).collect(Collectors.toList());
    }

    private SynchronisationConnectionStatus buildSynchronisationConnectionStatus(String croDomain) {
        SynchronisationConnectionStatus status = new SynchronisationConnectionStatus();
        status.setStatus(SynchronisationConnectionStatusType.MODIFIED);
        status.setCommonReferenceOperator(buildCommonReferenceOperator(croDomain));
        return status;
    }

    private CommonReferenceOperator buildCommonReferenceOperator(String croDomain) {
        CommonReferenceOperator commonReferenceOperator = new CommonReferenceOperator();
        commonReferenceOperator.setDomain(croDomain);
        return commonReferenceOperator;
    }

}
