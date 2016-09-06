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

package energy.usef.brp.workflow.plan.flexrequest.create;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PtuFlexRequestDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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

/**
 * Test class in charge of the unit tests related to the {@link BrpCreateFlexRequestCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class BrpCreateFlexRequestCoordinatorTest {

    private BrpCreateFlexRequestCoordinator coordinator;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private Config config;
    @Mock
    private JMSHelperService jmsHelperService;

    @Before
    public void init() {
        coordinator = new BrpCreateFlexRequestCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);
        PowerMockito.when(config.getProperty(Matchers.eq(ConfigParam.HOST_DOMAIN))).thenReturn("brp.usef-example.com");
    }

    @Test
    public void testHandleEventWorkflow() {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        coordinator.handleEvent(buildEvent());
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(messageCaptor.capture());
        String message = messageCaptor.getValue();
        Assert.assertNotNull(message);
    }

    private CreateFlexRequestEvent buildEvent() {
        List<FlexRequestDto> flexRequestDtos = new ArrayList<>();
        flexRequestDtos.add(buildFlexRequestDto());
        return new CreateFlexRequestEvent(flexRequestDtos);
    }

    private FlexRequestDto buildFlexRequestDto() {
        FlexRequestDto flexRequestDto = new FlexRequestDto();
        flexRequestDto.setParticipantDomain("agr.usef-example.com");
        flexRequestDto.setPeriod(DateTimeUtil.getCurrentDate());
        flexRequestDto.setPrognosisSequenceNumber(1l);
        IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuFlexRequestDto ptuFlexRequestDto = new PtuFlexRequestDto();
            ptuFlexRequestDto.setPtuIndex(BigInteger.valueOf(index));
            ptuFlexRequestDto.setPower(BigInteger.TEN);
            return ptuFlexRequestDto;
        }).forEach(ptu -> flexRequestDto.getPtus().add(ptu));
        return flexRequestDto;
    }
}
