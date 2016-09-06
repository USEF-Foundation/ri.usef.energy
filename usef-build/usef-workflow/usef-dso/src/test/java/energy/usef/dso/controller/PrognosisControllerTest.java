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

package energy.usef.dso.controller;

import energy.usef.core.config.Config;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.dso.exception.DsoBusinessError;
import energy.usef.dso.service.business.DsoPlanboardValidatorService;
import energy.usef.dso.workflow.plan.connection.forecast.DsoDPrognosisCoordinator;

import java.math.BigInteger;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link PrognosisController}.
 */
@RunWith(PowerMockRunner.class)
public class PrognosisControllerTest {

    private PrognosisController controller;

    @Mock
    private DsoPlanboardValidatorService dsoPlanboardValidatorService;
    @Mock
    private CorePlanboardValidatorService corePlanboardValidatorService;
    @Mock
    private DsoDPrognosisCoordinator coordinator;
    @Mock
    private Config config;
    @Mock
    private JMSHelperService jmsHelperService;

    @Before
    public void init() {
        controller = new PrognosisController();
        Whitebox.setInternalState(controller, dsoPlanboardValidatorService);
        Whitebox.setInternalState(controller, corePlanboardValidatorService);
        Whitebox.setInternalState(controller, coordinator);
        Whitebox.setInternalState(controller, config);
        Whitebox.setInternalState(controller, jmsHelperService);
    }

    @Test
    public void testActionWithSuccess() throws BusinessException {
        Prognosis prognosis = buildPrognosis();
        controller.action(prognosis, null);
        Mockito.verify(dsoPlanboardValidatorService, Mockito.times(1)).validateAggregator(
                Matchers.any(String.class), Matchers.any(String.class), Matchers.any(LocalDate.class));
    }

    @Test
    public void testActionWithPowerFailure() throws BusinessException {
        Prognosis prognosis = buildPrognosis();
        prognosis.getPTU().get(1).setPower(BigInteger.valueOf(100000000000L));
        controller.action(prognosis, null);
        Mockito.verify(dsoPlanboardValidatorService, Mockito.times(1)).validateAggregator(
                Matchers.any(String.class), Matchers.any(String.class), Matchers.any(LocalDate.class));
    }

    @Test
    public void testActionWithValidationErrors() throws BusinessException {
        Prognosis prognosis = buildPrognosis();
        PowerMockito.doThrow(new BusinessValidationException(DsoBusinessError.NON_EXISTING_CONGESTION_POINT)).when(
                dsoPlanboardValidatorService).validateCongestionPoint(Matchers.any(String.class));
        controller.action(prognosis, null);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(Matchers.any(String.class));
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
            ptu.setPower(BigInteger.valueOf(100));
            list.add(ptu);
        }
        return prognosis;
    }
}
