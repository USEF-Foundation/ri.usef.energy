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

package nl.energieprojecthoogdalem.agr.pbc;

import info.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesWorkflowParameter.IN;
import info.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesWorkflowParameter.OUT;
import info.usef.core.util.DateTimeUtil;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.PrognosisDto;
import info.usef.core.workflow.dto.PrognosisTypeDto;
import info.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to 'ReCreatePrognoses'.
 */
public class ReCreatePrognosesTest
{
    private ReCreatePrognoses reCreatePrognoses;

    private static final List<Long> dSequences = new ArrayList<>()
                                    ,aSequences = new ArrayList<>();

    @Before
    public void setUp()
    {
        dSequences.add(4L);
        dSequences.add(5L);

        aSequences.add(10L);
        aSequences.add(11L);

        reCreatePrognoses = new ReCreatePrognoses();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeRecreatePlans() throws Exception
    {
        WorkflowContext result = reCreatePrognoses.invoke(buildContext(BigInteger.valueOf(128), BigInteger.valueOf(64)));

        List<Long> dPrognosisSequencesList = result.get(OUT.REQUIRES_NEW_D_PROGNOSIS_SEQUENCES_LIST.name(), List.class);
        List<Long> aPlanSequencesList = result.get(OUT.REQUIRES_NEW_A_PLAN_SEQUENCES_LIST.name(), List.class);

        Assert.assertNotNull(dPrognosisSequencesList);
        Assert.assertNotNull(aPlanSequencesList);

        for (int i = 0; i < aSequences.size(); i++)
            Assert.assertEquals(aPlanSequencesList.get(i), aSequences.get(i));

        for (int i = 0; i < dSequences.size(); i++)
            Assert.assertEquals(dPrognosisSequencesList.get(i), dSequences.get(i));
    }



    private WorkflowContext buildContext( BigInteger dprognosisPower, BigInteger aplanPower)
    {
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(IN.LATEST_D_PROGNOSES_DTO_LIST.name(),
                    buildLatestPrognoses(PrognosisTypeDto.D_PROGNOSIS, "ean.123456789012345678", dprognosisPower, dSequences.toArray(new Long[dSequences.size()])));

        context.setValue(IN.LATEST_A_PLANS_DTO_LIST.name(),
                    buildLatestPrognoses(PrognosisTypeDto.A_PLAN, "ean.123456789012345678", aplanPower, aSequences.toArray(new Long[aSequences.size()])));

        return context;
    }

    private List<PrognosisDto> buildLatestPrognoses(PrognosisTypeDto prognosisType, String usefIdentifier,
            BigInteger prognosisPower, Long... sequences) {
        return Stream.of(sequences).map(sequence -> {
            PrognosisDto prognosisDto = new PrognosisDto();
            IntStream.rangeClosed(1, 12).mapToObj(index -> {
                PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
                ptuPrognosisDto.setPower(prognosisPower);
                ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(index));
                return ptuPrognosisDto;
            }).forEach(ptu -> prognosisDto.getPtus().add(ptu));
            prognosisDto.setType(prognosisType);
            prognosisDto.setSequenceNumber(sequence);
            prognosisDto.setParticipantDomain("agr.usef-example.com");
            prognosisDto.setConnectionGroupEntityAddress(usefIdentifier);
            prognosisDto.setPeriod(DateTimeUtil.getCurrentDate());
            return prognosisDto;
        }).collect(Collectors.toList());
    }

}
