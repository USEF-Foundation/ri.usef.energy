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

package energy.usef.brp.workflow.step;

import energy.usef.brp.pbcfeederimpl.PbcFeederService;
import energy.usef.brp.workflow.plan.connection.forecast.PrepareFlexRequestWorkflowParameter;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PrognosisTypeDto;
import energy.usef.core.workflow.dto.PtuFlexRequestDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link BrpPrepareFlexRequestsStub} class.
 */
@RunWith(PowerMockRunner.class)
public class BrpPrepareFlexRequestsStubTest {

    private static final Integer PTUS_PER_DAY = 6;
    private static final LocalDate PERIOD = new LocalDate(2015, 2, 12);
    private static final String AGR1_DOMAIN = "agr1.usef-example.com";

    @Mock
    private PbcFeederService pbcFeederService;

    private BrpPrepareFlexRequestsStub stub;

    private SequenceGeneratorService sequenceGeneratorService;


    @Before
    public void init() {
        sequenceGeneratorService = new SequenceGeneratorService();
        stub = new BrpPrepareFlexRequestsStub();
        Whitebox.setInternalState(stub, pbcFeederService);
        Mockito.when(pbcFeederService.retrieveApxPrices(Mockito.any(), Mockito.anyInt(), Mockito.anyInt()))
                .then(call -> IntStream.rangeClosed(1, PTUS_PER_DAY)
                        .mapToObj(Integer::valueOf)
                        .collect(Collectors.toMap(Function.identity(), i -> new BigDecimal("" + (i * 100)))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokePositivePowerSuccessful() {

        // apx prices 100 -> 600
        PrognosisDto aPlanDto = createAPlanDto();
        for (int i = 1; i <= PTUS_PER_DAY; i++) {
            PtuPrognosisDto ptuAPlanDto = new PtuPrognosisDto();
            ptuAPlanDto.setPtuIndex(BigInteger.valueOf(i));
            ptuAPlanDto.setPower(BigInteger.valueOf(200));
            aPlanDto.getPtus().add(ptuAPlanDto);
        }

        WorkflowContext context = stub.invoke(buildContext(aPlanDto));
        // verify that the step returns the context
        Assert.assertNotNull(context);

        // verify that we have at least an empty list of FlexRequests.
        List<FlexRequestDto> flexRequestMessages = (List<FlexRequestDto>) context
                .getValue(PrepareFlexRequestWorkflowParameter.OUT.FLEX_REQUEST_DTO_LIST.name());
        Assert.assertNotNull(flexRequestMessages);

        List<PrognosisDto> acceptedAPlans = (List<PrognosisDto>) context
                .getValue(PrepareFlexRequestWorkflowParameter.OUT.ACCEPTED_A_PLAN_DTO_LIST.name());
        Assert.assertNotNull(acceptedAPlans);

        Assert.assertEquals(1, flexRequestMessages.size());

        FlexRequestDto flexRequestDto = flexRequestMessages.get(0);

        // sort results
        flexRequestDto.getPtus().sort((o1, o2) -> o1.getPtuIndex().compareTo(o2.getPtuIndex()));

        // ptu 1 (index = 0) == cheap
        PtuFlexRequestDto cheap = flexRequestDto.getPtus().get(0);
        Assert.assertEquals(DispositionTypeDto.REQUESTED, cheap.getDisposition());
        Assert.assertEquals(BigInteger.valueOf(100), cheap.getPower());

        // middle part is average
        for (int i = 2; i <= 4; i++) {
            PtuFlexRequestDto dto = flexRequestDto.getPtus().get(i - 1);
            Assert.assertEquals(DispositionTypeDto.AVAILABLE, dto.getDisposition());
            Assert.assertEquals(BigInteger.ZERO, dto.getPower());
        }

        // ptu 5 and 6 == expensive
        PtuFlexRequestDto expensive = flexRequestDto.getPtus().get(5 - 1);
        Assert.assertEquals(DispositionTypeDto.REQUESTED, expensive.getDisposition());
        Assert.assertEquals(BigInteger.valueOf(-100), expensive.getPower());
        expensive = flexRequestDto.getPtus().get(6 - 1);
        Assert.assertEquals(DispositionTypeDto.REQUESTED, expensive.getDisposition());
        Assert.assertEquals(BigInteger.valueOf(-100), expensive.getPower());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeNegativePowerSuccessful() {

        // apx prices 100 -> 600
        PrognosisDto aPlanDto = createAPlanDto();
        for (int i = 1; i <= PTUS_PER_DAY; i++) {
            PtuPrognosisDto ptuAPlanDto = new PtuPrognosisDto();
            ptuAPlanDto.setPtuIndex(BigInteger.valueOf(i));
            ptuAPlanDto.setPower(BigInteger.valueOf(-200));
            aPlanDto.getPtus().add(ptuAPlanDto);
        }

        WorkflowContext context = stub.invoke(buildContext(aPlanDto));
        // verify that the step returns the context
        Assert.assertNotNull(context);

        // verify that we have at least an empty list of FlexRequests.
        List<FlexRequestDto> flexRequestMessages = (List<FlexRequestDto>) context
                .getValue(PrepareFlexRequestWorkflowParameter.OUT.FLEX_REQUEST_DTO_LIST.name());
        Assert.assertNotNull(flexRequestMessages);

        List<PrognosisDto> acceptedAPlans = (List<PrognosisDto>) context
                .getValue(PrepareFlexRequestWorkflowParameter.OUT.ACCEPTED_A_PLAN_DTO_LIST.name());
        Assert.assertNotNull(acceptedAPlans);

        Assert.assertEquals(1, flexRequestMessages.size());

        FlexRequestDto flexRequestDto = flexRequestMessages.get(0);

        // sort results
        flexRequestDto.getPtus().sort((o1, o2) -> o1.getPtuIndex().compareTo(o2.getPtuIndex()));

        // ptu 1 == cheap
        PtuFlexRequestDto cheap = flexRequestDto.getPtus().get(0);
        Assert.assertEquals(DispositionTypeDto.REQUESTED, cheap.getDisposition());
        Assert.assertEquals(BigInteger.valueOf(-100), cheap.getPower());

        // middle part is average
        for (int i = 2; i <= 4; i++) {
            PtuFlexRequestDto dto = flexRequestDto.getPtus().get(i - 1);
            Assert.assertEquals(DispositionTypeDto.AVAILABLE, dto.getDisposition());
            Assert.assertEquals(BigInteger.ZERO, dto.getPower());
        }

        // ptu 5 and 6 == expensive
        PtuFlexRequestDto expensive = flexRequestDto.getPtus().get(5 - 1);
        Assert.assertEquals(DispositionTypeDto.REQUESTED, expensive.getDisposition());
        Assert.assertEquals(BigInteger.valueOf(100), expensive.getPower());
        expensive = flexRequestDto.getPtus().get(6 - 1);
        Assert.assertEquals(DispositionTypeDto.REQUESTED, expensive.getDisposition());
        Assert.assertEquals(BigInteger.valueOf(100), expensive.getPower());
    }

    private PrognosisDto createAPlanDto() {
        PrognosisDto aPlanDto = new PrognosisDto();
        aPlanDto.setConnectionGroupEntityAddress(null);
        aPlanDto.setParticipantDomain(AGR1_DOMAIN);
        aPlanDto.setSequenceNumber(sequenceGeneratorService.next());
        aPlanDto.setPeriod(PERIOD);
        aPlanDto.setType(PrognosisTypeDto.A_PLAN);
        return aPlanDto;
    }

    private WorkflowContext buildContext(PrognosisDto prognosisDto) {
        DefaultWorkflowContext context = new DefaultWorkflowContext();
        context.setValue(PrepareFlexRequestWorkflowParameter.IN.PTU_DURATION.name(), 240);
        List<PrognosisDto> aPlanDtos = new ArrayList<>();
        aPlanDtos.add(prognosisDto);
        context.setValue(PrepareFlexRequestWorkflowParameter.IN.PROCESSED_A_PLAN_DTO_LIST.name(), aPlanDtos);
        return context;
    }

}
