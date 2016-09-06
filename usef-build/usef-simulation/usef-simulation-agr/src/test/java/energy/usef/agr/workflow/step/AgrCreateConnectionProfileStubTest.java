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

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.dto.ElementDtuDataDto;
import energy.usef.agr.dto.ElementTypeDto;
import energy.usef.agr.workflow.plan.connection.profile.CreateConnectionProfileStepParameter;
import energy.usef.agr.workflow.plan.connection.profile.CreateConnectionProfileStepParameter.IN;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class in charge of the unit tests related to the {@link AgrCreateConnectionProfileStubTest} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrCreateConnectionProfileStubTest {
    public static final String CONNECTION_ENTITY_ADDRESS = "ean.0000000001";
    private static final LocalDate period = DateTimeUtil.parseDate("2015-08-20");
    private AgrCreateConnectionProfileStub agrCreateConnectionProfileStub;


    @Before
    public void setUp() {
        agrCreateConnectionProfileStub = new AgrCreateConnectionProfileStub();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvokeWorkflowSucceeds() {
        // variables and mocking
        // invocation
        WorkflowContext outContext = agrCreateConnectionProfileStub.invoke(buildInputContext());

        // verifications
        Assert.assertNotNull(outContext);
        List<ConnectionPortfolioDto> connections = (List<ConnectionPortfolioDto>) outContext
                .getValue(CreateConnectionProfileStepParameter.OUT.CONNECTION_PORTFOLIO_DTO_LIST.name());
        Assert.assertEquals(1, connections.size());
        //verify the average mechanism for the first ptu
        Assert.assertEquals(BigInteger.valueOf(3),
                connections.get(0).getConnectionPowerPerPTU().get(1).getProfile().getAverageConsumption());
        Assert.assertEquals(BigInteger.valueOf(3),
                connections.get(0).getConnectionPowerPerPTU().get(1).getProfile().getAverageProduction());
        Assert.assertEquals(BigInteger.valueOf(3),
                connections.get(0).getConnectionPowerPerPTU().get(1).getProfile().getPotentialFlexConsumption());
        Assert.assertEquals(BigInteger.valueOf(3),
                connections.get(0).getConnectionPowerPerPTU().get(1).getProfile().getPotentialFlexProduction());

        // for each connection, verify that the profile value is filled in.
        connections.forEach(connectionPortfolioDTO -> Assert.assertNotEquals(0,
                connectionPortfolioDTO.getConnectionPowerPerPTU().size()));
        connections.forEach(connectionPortfolioDTO -> Assert.assertTrue(
                connectionPortfolioDTO.getConnectionPowerPerPTU().entrySet().stream()
                        .noneMatch(entry -> entry.getValue().getProfile() == null)));
    }

    private WorkflowContext buildInputContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), buildConnectionPortfolio());
        context.setValue(IN.ELEMENT_PER_CONNECTION_MAP.name(), buildElementMap());
        context.setValue(IN.PERIOD.name(), period);
        context.setValue(IN.PTU_DURATION.name(), 15);
        return context;
    }

    private Map<String, List<ElementDto>> buildElementMap() {

        Map<String, List<ElementDto>> result = new HashMap<>();
        result.put(CONNECTION_ENTITY_ADDRESS, buildElementList());
        return result;
    }

    private List<ElementDto> buildElementList() {
        ElementDto element = new ElementDto();
        element.setId("ADS1");
        element.setDtuDuration(5);
        element.setConnectionEntityAddress("conn.1");
        element.setElementType(ElementTypeDto.MANAGED_DEVICE);
        element.setProfile("PBCFeeder");

        element.getElementDtuData().add(createElementDtuData(1, BigInteger.ONE));
        element.getElementDtuData().add(createElementDtuData(2, BigInteger.TEN));
        element.getElementDtuData().add(createElementDtuData(3, BigInteger.ZERO));

        List<ElementDto> elementList = new ArrayList<>();
        elementList.add(element);
        return elementList;
    }

    private ElementDtuDataDto createElementDtuData(int index, BigInteger power) {
        ElementDtuDataDto elementDtuDataDto = new ElementDtuDataDto();
        elementDtuDataDto.setDtuIndex(index);
        elementDtuDataDto.setProfileAverageConsumption(power);
        elementDtuDataDto.setProfileAverageProduction(power);
        elementDtuDataDto.setProfilePotentialFlexConsumption(power);
        elementDtuDataDto.setProfilePotentialFlexProduction(power);
        return elementDtuDataDto;
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolio() {
        return Collections.singletonList(new ConnectionPortfolioDto(CONNECTION_ENTITY_ADDRESS));
    }

}
