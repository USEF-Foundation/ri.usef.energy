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

package energy.usef.brp.workflow.plan.connection.forecast;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.brp.model.CommonReferenceOperator;
import energy.usef.brp.service.business.BrpBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;

import java.util.Arrays;
import java.util.List;

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
 * Test class in charge of the unit tests related to the {@link BrpCommonReferenceQueryCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class BrpCommonReferenceQueryCoordinatorTest {

    @Mock
    private JMSHelperService jmsHelperService;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private BrpBusinessService brpBusinessService;

    @Mock
    private Config config;

    @Mock
    private ConfigBrp configBrp;

    private BrpCommonReferenceQueryCoordinator coordinator;

    @Before
    public void init() {
        coordinator = new BrpCommonReferenceQueryCoordinator();
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configBrp);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, brpBusinessService);
    }

    @Test
    public void testHandleEventSuccessfully() {
        PowerMockito.when(brpBusinessService.findAllCommonReferenceOperators()).thenReturn(buildCommonReferenceOperatorList());
        PowerMockito.when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("brp.usef-example.com");
        PowerMockito.when(configBrp.getIntegerProperty(ConfigBrpParam.BRP_INITIALIZE_PLANBOARD_DAYS_INTERVAL)).thenReturn(1);

        coordinator.handleEvent(new CommonReferenceQueryEvent());

        Mockito.verify(brpBusinessService, Mockito.times(1)).findAllCommonReferenceOperators();
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(2)).sendMessageToOutQueue(messageCaptor.capture());

        List<String> sentMessages = messageCaptor.getAllValues();
        Assert.assertEquals(2, sentMessages.size());
        Assert.assertTrue(sentMessages.get(0).contains("cro1.usef-example.com"));
        Assert.assertTrue(sentMessages.get(1).contains("cro2.usef-example.com"));
    }

    private List<CommonReferenceOperator> buildCommonReferenceOperatorList() {
        CommonReferenceOperator cro1 = new CommonReferenceOperator();
        cro1.setId(-1l);
        cro1.setDomain("cro1.usef-example.com");
        CommonReferenceOperator cro2 = new CommonReferenceOperator();
        cro2.setId(-2l);
        cro2.setDomain("cro2.usef-example.com");
        return Arrays.asList(cro1, cro2);
    }

}
