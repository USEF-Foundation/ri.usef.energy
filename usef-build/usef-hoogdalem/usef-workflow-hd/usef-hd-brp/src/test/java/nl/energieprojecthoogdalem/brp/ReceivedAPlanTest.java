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

package nl.energieprojecthoogdalem.brp;

import info.usef.brp.workflow.plan.connection.forecast.ReceivedAPlanWorkflowParameter;
import info.usef.core.service.business.SequenceGeneratorService;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.PrognosisDto;
import info.usef.core.workflow.dto.PrognosisTypeDto;
import info.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class in charge of the unit tests related to the {@link ReceivedAPlan} class.
 */
@RunWith(PowerMockRunner.class)
public class ReceivedAPlanTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceivedAPlanTest.class);
    private static final Integer PTUS_PER_DAY = 6;
    private static final LocalDate PERIOD = new LocalDate(2015, 2, 12);
    private static final String AGR1_DOMAIN = "agr1.usef-example.com";
    private static final String AGR2_DOMAIN = "agr2.usef-example.com";
    private SequenceGeneratorService sequenceGeneratorService;
    private ReceivedAPlan brpReceivedAPlanStub;

    @Before
    public void init() {
        brpReceivedAPlanStub = new ReceivedAPlan();
        sequenceGeneratorService = new SequenceGeneratorService();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvoke() {
        LOGGER.info("ReceivedAPlanTest invoked.");

        WorkflowContext context = brpReceivedAPlanStub.invoke(buildContext());

        List<PrognosisDto> acceptedAPlans = (List<PrognosisDto>) context
                .getValue(ReceivedAPlanWorkflowParameter.OUT.ACCEPTED_A_PLAN_DTO_LIST.name());
        List<PrognosisDto> processedAPlans = (List<PrognosisDto>) context
                .getValue(ReceivedAPlanWorkflowParameter.OUT.PROCESSED_A_PLAN_DTO_LIST.name());
        Assert.assertNotNull(acceptedAPlans);
        Assert.assertNotNull(processedAPlans);

        // there is some random
        Assert.assertEquals(2, acceptedAPlans.size());
        Assert.assertEquals(0, processedAPlans.size());

    }

    private WorkflowContext buildContext() {
        DefaultWorkflowContext context = new DefaultWorkflowContext();
        context.setValue(ReceivedAPlanWorkflowParameter.IN.PTU_DURATION.name(), 240);
        List<PrognosisDto> aplanList = buildAplanList();
        context.setValue(ReceivedAPlanWorkflowParameter.IN.RECEIVED_A_PLAN_DTO_LIST.name(), aplanList);
        List<PrognosisDto> fullAplanList = new ArrayList<>(aplanList);
        fullAplanList.add(buildAplan(AGR1_DOMAIN));
        context.setValue(ReceivedAPlanWorkflowParameter.IN.A_PLAN_DTO_LIST.name(), fullAplanList);
        return context;
    }

    private List<PrognosisDto> buildAplanList() {
        List<PrognosisDto> aplansDto = new ArrayList<>();
        aplansDto.add(buildAplan(AGR1_DOMAIN));
        aplansDto.add(buildAplan(AGR2_DOMAIN));
        return aplansDto;
    }

    private PrognosisDto buildAplan(String aggregatorDomain) {
        PrognosisDto aplanDto = new PrognosisDto();
        aplanDto.setConnectionGroupEntityAddress(null);
        aplanDto.setSequenceNumber(sequenceGeneratorService.next());
        aplanDto.setPeriod(PERIOD);
        aplanDto.setType(PrognosisTypeDto.A_PLAN);
        aplanDto.setParticipantDomain(aggregatorDomain);
        for (int i = 1; i <= PTUS_PER_DAY; ++i) {
            PtuPrognosisDto ptuAplanDto = new PtuPrognosisDto();
            ptuAplanDto.setPtuIndex(BigInteger.valueOf(i));
            ptuAplanDto.setPower(BigInteger.valueOf(100).multiply(BigInteger.valueOf(i)));
            aplanDto.getPtus().add(ptuAplanDto);
        }
        return aplanDto;
    }

}
