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

package energy.usef.agr.workflow.step;

import energy.usef.agr.workflow.nonudi.dto.CongestionManagementProfileDto;
import energy.usef.agr.workflow.nonudi.dto.ObjectiveAgentProfileDto;
import energy.usef.agr.workflow.nonudi.goals.AgrNonUdiSetAdsGoalsParameter;
import energy.usef.agr.workflow.nonudi.service.PowerMatcher;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PrognosisTypeDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrNonUdiSetAdsGoalsStub} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrNonUdiSetAdsGoalsStubTest {

    private final static String PARTICIPANT_DOMAIN = "brp1.usef-example.com";
    private AgrNonUdiSetAdsGoalsStub agrNonUdiSetAdsGoalsStub;
    @Mock
    private PowerMatcher powerMatcher;

    @Before
    public void init() {
        agrNonUdiSetAdsGoalsStub = new AgrNonUdiSetAdsGoalsStub();
        Whitebox.setInternalState(agrNonUdiSetAdsGoalsStub, powerMatcher);
    }

    @Test
    public void testInvokeForAPlans() throws Exception {
        final String brpIdentifier = "brp1.usef-example.com";
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(AgrNonUdiSetAdsGoalsParameter.IN.PERIOD.name(), DateTimeUtil.getCurrentDate());
        context.setValue(AgrNonUdiSetAdsGoalsParameter.IN.PTU_DURATION.name(), 15);
        context.setValue(AgrNonUdiSetAdsGoalsParameter.IN.PROGNOSIS_DTO.name(),
                buildPrognosisDto(PrognosisTypeDto.A_PLAN, brpIdentifier));

        WorkflowContext outContext = agrNonUdiSetAdsGoalsStub.invoke(context);
        ArgumentCaptor<String> usefIdentifierCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> objectiveAgentProfileDtosCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(powerMatcher, Mockito.times(1))
                .postObjectiveAgent(usefIdentifierCaptor.capture(), objectiveAgentProfileDtosCaptor.capture());

        Assert.assertEquals(brpIdentifier, usefIdentifierCaptor.getValue());
        List<ObjectiveAgentProfileDto> capturedList = objectiveAgentProfileDtosCaptor.getValue();
        Assert.assertEquals(96, capturedList.size());

        // verify ptu 1
        Assert.assertEquals(PowerMatcher.getInterval(DateTimeUtil.getCurrentDate(), 1, 15, 1),
                capturedList.get(0).getTimeInterval());
        Assert.assertEquals(BigDecimal.valueOf(1).multiply(BigDecimal.TEN), capturedList.get(0).getTargetDemandWatt());

        // verify ptu 96
        Assert.assertEquals(PowerMatcher.getInterval(DateTimeUtil.getCurrentDate(), 96, 15, 1),
                capturedList.get(95).getTimeInterval());
        Assert.assertEquals(BigDecimal.valueOf(96).multiply(BigDecimal.TEN), capturedList.get(95).getTargetDemandWatt());
    }

    @Test
    public void testInvokeForDPrognoses() throws Exception {
        final String congestionPoint = "ean.1111111111";
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(AgrNonUdiSetAdsGoalsParameter.IN.PERIOD.name(), DateTimeUtil.getCurrentDate());
        context.setValue(AgrNonUdiSetAdsGoalsParameter.IN.PTU_DURATION.name(), 15);
        context.setValue(AgrNonUdiSetAdsGoalsParameter.IN.PROGNOSIS_DTO.name(),
                buildPrognosisDto(PrognosisTypeDto.D_PROGNOSIS, congestionPoint));

        WorkflowContext outContext = agrNonUdiSetAdsGoalsStub.invoke(context);
        ArgumentCaptor<String> usefIdentifierCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> congestionManagementProfileDtoCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(powerMatcher, Mockito.times(1))
                .postCongestionManagement(usefIdentifierCaptor.capture(), congestionManagementProfileDtoCaptor.capture());

        Assert.assertEquals(congestionPoint, usefIdentifierCaptor.getValue());
        List<CongestionManagementProfileDto> capturedList = congestionManagementProfileDtoCaptor.getValue();
        Assert.assertEquals(96, capturedList.size());

        // verify ptu 1
        Assert.assertEquals(PowerMatcher.getInterval(DateTimeUtil.getCurrentDate(), 1, 15, 1),
                capturedList.get(0).getTimeInterval());
        Assert.assertEquals(BigDecimal.valueOf(1).multiply(BigDecimal.TEN), capturedList.get(0).getMinDemandWatt());
        Assert.assertEquals(BigDecimal.valueOf(1).multiply(BigDecimal.TEN), capturedList.get(0).getMaxDemandWatt());

        // verify ptu 96
        Assert.assertEquals(PowerMatcher.getInterval(DateTimeUtil.getCurrentDate(), 96, 15, 1),
                capturedList.get(95).getTimeInterval());
        Assert.assertEquals(BigDecimal.valueOf(96).multiply(BigDecimal.TEN), capturedList.get(95).getMinDemandWatt());
        Assert.assertEquals(BigDecimal.valueOf(96).multiply(BigDecimal.TEN), capturedList.get(95).getMaxDemandWatt());
    }

    private PrognosisDto buildPrognosisDto(PrognosisTypeDto type, String usefIdentifier) {
        PrognosisDto prognosisDto = new PrognosisDto();
        prognosisDto.setPeriod(new LocalDate());
        prognosisDto.setConnectionGroupEntityAddress(usefIdentifier);
        prognosisDto.setParticipantDomain(PARTICIPANT_DOMAIN);
        prognosisDto.setSequenceNumber(1l);
        prognosisDto.setType(type);
        prognosisDto.setPtus(buildPtuPrognoses());
        return prognosisDto;
    }

    private List<PtuPrognosisDto> buildPtuPrognoses() {
        List<PtuPrognosisDto> ptuPrognoses = new ArrayList<>();

        IntStream.rangeClosed(1, 96).forEach(i -> {
            PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto(BigInteger.valueOf(i),
                    BigInteger.valueOf(i).multiply(BigInteger.TEN));
            ptuPrognoses.add(ptuPrognosisDto);
        });

        return ptuPrognoses;
    }
}
