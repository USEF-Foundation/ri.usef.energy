/*
 * This software source code is provided by the USEF Foundation. The copyright
 * and all other intellectual property rights relating to all software source
 * code provided by the USEF Foundation (and changes and modifications as well
 * as on new versions of this software source code) belong exclusively to the
 * USEF Foundation and/or its suppliers or licensors. Total or partial
 * transfer of such a right is not allowed. The user of the software source
 * code made available by USEF Foundation acknowledges these rights and will
 * refrain from any form of infringement of these rights.
 *
 * The USEF Foundation provides this software source code "as is". In no event
 * shall the USEF Foundation and/or its suppliers or licensors have any
 * liability for any incidental, special, indirect or consequential damages;
 * loss of profits, revenue or data; business interruption or cost of cover or
 * damages arising out of or in connection with the software source code or
 * accompanying documentation.
 *
 * For the full license agreement see http://www.usef.info/license.
 */

package nl.energieprojecthoogdalem.agr.pbc;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.ElementDto;
import info.usef.agr.dto.ElementDtuDataDto;
import info.usef.agr.dto.ElementTypeDto;
import info.usef.agr.workflow.plan.connection.profile.CreateConnectionProfileStepParameter;
import info.usef.agr.workflow.plan.connection.profile.CreateConnectionProfileStepParameter.IN;
import info.usef.core.util.DateTimeUtil;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigInteger;
import java.util.*;

/**
 * Test class in charge of the unit tests related to the {@link CreateConnectionProfileTest} class.
 */
@RunWith(PowerMockRunner.class)
public class CreateConnectionProfileTest {
    public static final String CONNECTION_ENTITY_ADDRESS = "ean.0000000001";
    private static final LocalDate period = DateTimeUtil.parseDate("2015-08-20");
    private CreateConnectionProfile createConnectionProfile;


    @Before
    public void setUp() {
        createConnectionProfile = new CreateConnectionProfile();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvokeWorkflowSucceeds() {
        // variables and mocking
        // invocation
        WorkflowContext outContext = createConnectionProfile.invoke(buildInputContext());

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
