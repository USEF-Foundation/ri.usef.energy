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

import energy.usef.agr.PortfolioBuilder;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesWorkflowParameter.IN;
import energy.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesWorkflowParameter.OUT;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PrognosisTypeDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.api.client.util.ArrayMap;

/**
 * Test class in charge of the unit tests related to the Stub 'AGRReCreatePrognoses'.
 */
public class AgrReCreatePrognosesStubTest {

    private static final int PTU_SIZE = 120;
    private static final int DTU_SIZE = 60;
    private static final int PTUS_PER_DAY = 12;
    private static final int DTUS_PER_PTU = 2;
    public static final LocalDate CURRENT_DATE = DateTimeUtil.getCurrentDate();

    private AgrReCreatePrognosesStub stub;

    @Before
    public void setUp() {
        stub = new AgrReCreatePrognosesStub();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithDeviationForDPrognoses() throws Exception {
        // 128 bigger than 84 +- floor(4.2) --> expect recreation
        WorkflowContext context = buildContext(USEFRole.DSO, BigInteger.valueOf(128), null);
        context = stub.invoke(context);
        List<Long> dPrognosisSequencesList = (List<Long>) context.getValue(OUT.REQUIRES_NEW_D_PROGNOSIS_SEQUENCES_LIST.name());
        List<Long> aPlanSequencesList = (List<Long>) context.getValue(OUT.REQUIRES_NEW_A_PLAN_SEQUENCES_LIST.name());
        Assert.assertNotNull(dPrognosisSequencesList);
        Assert.assertNotNull(aPlanSequencesList);
        dPrognosisSequencesList.sort(Long::compareTo);
        Assert.assertEquals(2, dPrognosisSequencesList.size());
        Assert.assertEquals(4l, dPrognosisSequencesList.get(0).longValue());
        Assert.assertEquals(5l, dPrognosisSequencesList.get(1).longValue());
        Assert.assertTrue(aPlanSequencesList.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithDeviationForAPlans() throws Exception {
        // 1085 > 84 + 1000 which is the max threshold
        WorkflowContext context = buildContext(USEFRole.BRP, null, BigInteger.valueOf(1085));
        context = stub.invoke(context);
        List<Long> dPrognosisSequencesList = (List<Long>) context.getValue(OUT.REQUIRES_NEW_D_PROGNOSIS_SEQUENCES_LIST.name());
        List<Long> aPlanSequencesList = (List<Long>) context.getValue(OUT.REQUIRES_NEW_A_PLAN_SEQUENCES_LIST.name());
        Assert.assertNotNull(dPrognosisSequencesList);
        Assert.assertNotNull(aPlanSequencesList);
        aPlanSequencesList.sort(Long::compareTo);
        Assert.assertTrue(dPrognosisSequencesList.isEmpty());
        Assert.assertEquals(3, aPlanSequencesList.size());
        Assert.assertEquals(1l, aPlanSequencesList.get(0).longValue());
        Assert.assertEquals(2l, aPlanSequencesList.get(1).longValue());
        Assert.assertEquals(3l, aPlanSequencesList.get(2).longValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithNoDeviation() throws Exception {
        // Prognosis power limit: 84 +- floor(4.2) to stay within 5% difference
        WorkflowContext context = buildContext(USEFRole.DSO, BigInteger.valueOf(80), BigInteger.valueOf(88));
        context = stub.invoke(context);
        List<Long> dPrognosisSequencesList = (List<Long>) context.getValue(OUT.REQUIRES_NEW_D_PROGNOSIS_SEQUENCES_LIST.name());
        List<Long> aPlanSequencesList = (List<Long>) context.getValue(OUT.REQUIRES_NEW_A_PLAN_SEQUENCES_LIST.name());
        Assert.assertNotNull(dPrognosisSequencesList);
        Assert.assertNotNull(aPlanSequencesList);
        Assert.assertTrue(dPrognosisSequencesList.isEmpty());
        Assert.assertTrue(aPlanSequencesList.isEmpty());
    }

    private WorkflowContext buildContext(USEFRole usefRole, BigInteger dprognosisPower, BigInteger aplanPower) {
        WorkflowContext context = new DefaultWorkflowContext();
        if (usefRole == USEFRole.DSO) {
            context.setValue(IN.LATEST_D_PROGNOSES_DTO_LIST.name(),
                    buildLatestPrognoses(PrognosisTypeDto.D_PROGNOSIS, "ean.123456789012345678", dprognosisPower, 4l, 5l));
            context.setValue(IN.CURRENT_PORTFOLIO.name(), buildConnectionPortfolio2());
            context.setValue(IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(),
                    buildConnectionGroupsToConnectionsMap("ean.123456789012345678",
                            context.get(IN.CURRENT_PORTFOLIO.name(), List.class)));
            context.setValue(IN.LATEST_A_PLANS_DTO_LIST.name(), new ArrayList<PrognosisDto>());

        }
        if (usefRole == USEFRole.BRP) {
            context.setValue(IN.LATEST_A_PLANS_DTO_LIST.name(),
                    buildLatestPrognoses(PrognosisTypeDto.A_PLAN, "brp.usef-example.com", aplanPower, 1l, 2l, 3l));
            context.setValue(IN.CURRENT_PORTFOLIO.name(), buildConnectionPortfolio2());
            context.setValue(IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(),
                    buildConnectionGroupsToConnectionsMap("brp.usef-example.com",
                            context.get(IN.CURRENT_PORTFOLIO.name(), List.class)));
            context.setValue(IN.LATEST_D_PROGNOSES_DTO_LIST.name(), new ArrayList<PrognosisDto>());

        }
        context.setValue(IN.PERIOD.name(), CURRENT_DATE);
        context.setValue(IN.PTU_DURATION.name(), PTU_SIZE);
        return context;
    }

    private Map<String, List<String>> buildConnectionGroupsToConnectionsMap(String usefIdentifier,
            List<ConnectionPortfolioDto> connectionPortfolioDTOs) {
        Map<String, List<String>> connectionGroupsToConnectionsMap = new ArrayMap<>();

        connectionPortfolioDTOs.forEach(connectionPortfolioDTO -> {
            if (!connectionGroupsToConnectionsMap.containsKey(usefIdentifier)) {
                connectionGroupsToConnectionsMap.put(usefIdentifier, new ArrayList<>());
            }

            connectionGroupsToConnectionsMap.get(usefIdentifier).add(connectionPortfolioDTO.getConnectionEntityAddress());
        });

        return connectionGroupsToConnectionsMap;
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

    private List<ConnectionPortfolioDto> buildConnectionPortfolio2() {
        String connection1 = "ean.000000000001";
        String connection2 = "ean.000000000002";
        return new PortfolioBuilder(CURRENT_DATE, 120, 60)
                .withConnection(connection1)
                .withConnection(connection2)
                .uncontrolledLoadForConnection(connection1, 16)
                .uncontrolledLoadForConnection(connection2, 16)
                .udisForConnections(connection2, 2, "udi://endpoint.", 13)
                .build().stream().collect(Collectors.toList());
    }
}
