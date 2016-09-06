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

package energy.usef.brp.controller;

import energy.usef.brp.exception.BrpBusinessError;
import energy.usef.brp.service.business.BrpPlanboardValidatorService;
import energy.usef.brp.workflow.plan.connection.forecast.ReceivedAPlanEvent;
import energy.usef.core.config.Config;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.validation.CorePlanboardValidatorService;

import java.math.BigInteger;
import java.util.List;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link APlanController}.
 */
@RunWith(PowerMockRunner.class)
public class APlanControllerTest {

    private APlanController controller;

    @Mock
    private BrpPlanboardValidatorService brpPlanboardValidatorService;
    @Mock
    private CorePlanboardValidatorService corePlanboardValidatorService;
    @Mock
    private Config config;
    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private Event<ReceivedAPlanEvent> receivedAPlanEventManager;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Before
    public void init() {
        controller = new APlanController();
        Whitebox.setInternalState(controller, brpPlanboardValidatorService);
        Whitebox.setInternalState(controller, corePlanboardValidatorService);
        Whitebox.setInternalState(controller, config);
        Whitebox.setInternalState(controller, jmsHelperService);
        Whitebox.setInternalState(controller, receivedAPlanEventManager);
        Whitebox.setInternalState(controller, corePlanboardBusinessService);
    }

    @Test
    public void testActionWithSuccess() throws BusinessException {
        Prognosis prognosis = buildPrognosis();
        controller.action(prognosis, null);
        Mockito.verify(brpPlanboardValidatorService, Mockito.times(1)).validatePeriod(
                Matchers.any(LocalDate.class));
        Mockito.verify(receivedAPlanEventManager, Mockito.times(1)).fire(Matchers.any(ReceivedAPlanEvent.class));
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).archiveAPlans(Matchers.any(String.class),
                Matchers.any(LocalDate.class));
    }

    @Test
    public void testActionWithValidationFailure() throws BusinessException {
        Prognosis prognosis = buildPrognosis();
        Mockito.doThrow(new BusinessValidationException(BrpBusinessError.INVALID_PERIOD))
                .when(brpPlanboardValidatorService).validatePeriod(Matchers.any(LocalDate.class));
        controller.action(prognosis, null);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(Matchers.any(String.class));
        Mockito.verify(corePlanboardBusinessService, Mockito.times(0)).archiveAPlans(Matchers.any(String.class),
                Matchers.any(LocalDate.class));
    }

    private Prognosis buildPrognosis() {
        Prognosis prognosis = new Prognosis();
        MessageMetadata metadata = new MessageMetadata();
        metadata.setSenderDomain("acm.com");
        prognosis.setMessageMetadata(metadata);
        prognosis.setCongestionPoint("abc");
        prognosis.setPeriod(new LocalDate());
        prognosis.setPTUDuration(Period.minutes(15));
        List<PTU> list = prognosis.getPTU();
        for (int i = 0; i < 96; ++i) {
            PTU ptu = new PTU();
            ptu.setStart(BigInteger.valueOf(i + 1));
            ptu.setPower(BigInteger.valueOf(100));
            list.add(ptu);
        }

        return prognosis;
    }
}
