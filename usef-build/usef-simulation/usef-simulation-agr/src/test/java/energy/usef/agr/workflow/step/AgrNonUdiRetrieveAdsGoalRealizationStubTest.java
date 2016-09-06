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

import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.workflow.nonudi.dto.CongestionManagementStatusDto;
import energy.usef.agr.workflow.nonudi.dto.ObjectiveAgentStatusDto;
import energy.usef.agr.workflow.nonudi.service.PowerMatcher;
import energy.usef.agr.workflow.nonudi.operate.AgrNonUdiRetrieveAdsGoalRealizationParameter;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.USEFRoleDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class AgrNonUdiRetrieveAdsGoalRealizationStubTest {

    public static final LocalDateTime LAST_UPDATE_TIME = new LocalDateTime(2015, 9, 7, 16, 5, 0);
    private AgrNonUdiRetrieveAdsGoalRealizationStub agrNonUdiRetrieveAdsGoalRealizationStub;

    @Mock
    private PowerMatcher powerMatcher;

    @Before
    public void setUp() throws Exception {
        agrNonUdiRetrieveAdsGoalRealizationStub = new AgrNonUdiRetrieveAdsGoalRealizationStub();
        Whitebox.setInternalState(agrNonUdiRetrieveAdsGoalRealizationStub, powerMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvoke() throws Exception {
        // variables and additional mocking
        final LocalDate period = new LocalDate(2015, 9, 7);
        final Integer ptuDuration = 15;
        WorkflowContext inputContext = buildInputContext(period, ptuDuration);
        Mockito.when(powerMatcher.retrieveBrpAdsGoalRealization(Matchers.any(String.class))).thenReturn(
                buildBrpAdsGoalRealization());
        Mockito.when(powerMatcher.retrieveCongestionPointAdsGoalRealization(Matchers.any(String.class))).thenReturn(
                buildCongestionPointAdsGoalRealization());
        // invocation
        WorkflowContext outContext = agrNonUdiRetrieveAdsGoalRealizationStub.invoke(inputContext);
        // verifications and assertions
        Mockito.verify(powerMatcher, Mockito.times(1)).retrieveCongestionPointAdsGoalRealization(Matchers.eq("ean.111111111111"));
        Mockito.verify(powerMatcher, Mockito.times(1)).retrieveBrpAdsGoalRealization(Matchers.eq("brp.usef-example.com"));
        Assert.assertNotNull(outContext);
        int wantedPtuIndex = PtuUtil.getPtuIndex(new LocalDateTime(), ptuDuration);
        List<ConnectionGroupPortfolioDto> updatedPortfolio = outContext.get(AgrNonUdiRetrieveAdsGoalRealizationParameter.OUT.UPDATED_PORTFOLIO.name(), List.class);
        Assert.assertNotNull(updatedPortfolio);
        Assert.assertEquals(BigInteger.valueOf(2000L),
                updatedPortfolio.get(0).getConnectionGroupPowerPerPTU().get(wantedPtuIndex).getObserved().calculatePower());
        Assert.assertEquals(BigInteger.valueOf(1000L), updatedPortfolio.get(1).getConnectionGroupPowerPerPTU().get(wantedPtuIndex)
                .getObserved().calculatePower());

    }

    private WorkflowContext buildInputContext(LocalDate period, Integer ptuDuration) {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.PERIOD.name(), period);
        context.setValue(AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.PTU_DURATION.name(), ptuDuration);
        context.setValue(AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.CURRENT_PORTFOLIO.name(), buildCurrentPortfolio());
        return context;
    }

    private List<ConnectionGroupPortfolioDto> buildCurrentPortfolio() {
        ConnectionGroupPortfolioDto congestionPointPortfolio = new ConnectionGroupPortfolioDto("ean.111111111111", USEFRoleDto.DSO);
        ConnectionGroupPortfolioDto brpPortfolio = new ConnectionGroupPortfolioDto("brp.usef-example.com", USEFRoleDto.BRP);
        return Arrays.asList(congestionPointPortfolio, brpPortfolio);
    }

    private Optional<ObjectiveAgentStatusDto> buildBrpAdsGoalRealization() {
        ObjectiveAgentStatusDto objectiveAgentStatusDto = new ObjectiveAgentStatusDto();
        objectiveAgentStatusDto.setCurrentAllocation(BigDecimal.valueOf(1000D));
        objectiveAgentStatusDto.setLastUpdate(LAST_UPDATE_TIME);
        return Optional.of(objectiveAgentStatusDto);
    }

    private Optional<CongestionManagementStatusDto> buildCongestionPointAdsGoalRealization() {
        CongestionManagementStatusDto congestionManagementStatusDto = new CongestionManagementStatusDto();
        congestionManagementStatusDto.setCurrentAllocation(BigDecimal.valueOf(2000D));
        congestionManagementStatusDto.setLastUpdate(LAST_UPDATE_TIME);
        return Optional.of(congestionManagementStatusDto);
    }
}
