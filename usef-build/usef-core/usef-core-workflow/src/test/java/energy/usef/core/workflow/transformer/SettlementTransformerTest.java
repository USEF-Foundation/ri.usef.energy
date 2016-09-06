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

package energy.usef.core.workflow.transformer;

import energy.usef.core.data.xml.bean.message.FlexOrderSettlement;
import energy.usef.core.data.xml.bean.message.PTUSettlement;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuSettlement;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.PtuSettlementDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link SettlementTransformer} class
 */
public class SettlementTransformerTest {

    private static final LocalDate PERIOD = new LocalDate(2015, 10, 21);
    public static final CongestionPointConnectionGroup CONNECTION_GROUP = new CongestionPointConnectionGroup("ean.000000000001");

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, SettlementTransformer.class.getDeclaredConstructors().length);
        Constructor<SettlementTransformer> constructor = SettlementTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransformToXml() {
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentType(DocumentType.FLEX_ORDER);
        planboardMessage.setPeriod(PERIOD);
        planboardMessage.setSequence(123456789L);

        energy.usef.core.model.FlexOrderSettlement flexOrderSettlement = new energy.usef.core.model.FlexOrderSettlement();
        flexOrderSettlement.setConnectionGroup(new CongestionPointConnectionGroup("ean.000000000001"));
        flexOrderSettlement.setFlexOrder(planboardMessage);
        flexOrderSettlement.setPeriod(new LocalDate(2015, 10, 21));

        FlexOrderSettlement flexOrderSettlementXml = SettlementTransformer.transformToXml(flexOrderSettlement);
        Assert.assertNotNull(flexOrderSettlementXml);
        Assert.assertEquals("ean.000000000001", flexOrderSettlementXml.getCongestionPoint());
        Assert.assertEquals(new LocalDate(2015, 10, 21), flexOrderSettlementXml.getPeriod());
        Assert.assertEquals("123456789", flexOrderSettlementXml.getOrderReference());

    }

    @Test
    public void testTransformPtuToXml() throws Exception {
        PtuContainer ptuContainer = new PtuContainer(new LocalDate(2015, 10, 21), 48);
        ConnectionGroup connectionGroup = CONNECTION_GROUP;
        PtuSettlement ptuSettlement = new PtuSettlement();
        ptuSettlement.setPtuContainer(ptuContainer);
        ptuSettlement.setConnectionGroup(connectionGroup);
        ptuSettlement.setActualPower(BigInteger.valueOf(2000));
        ptuSettlement.setPrognosisPower(BigInteger.valueOf(1000));
        ptuSettlement.setDeliveredFlexPower(BigInteger.valueOf(1000));
        ptuSettlement.setOrderedFlexPower(BigInteger.valueOf(2000));
        ptuSettlement.setFlexOrderSettlement(new energy.usef.core.model.FlexOrderSettlement());
        ptuSettlement.setPowerDeficiency(BigInteger.valueOf(1000));
        ptuSettlement.setPenalty(BigDecimal.valueOf(20));
        ptuSettlement.setPrice(BigDecimal.valueOf(50));

        PTUSettlement ptuSettlementXml = SettlementTransformer.transformToXml(ptuSettlement);
        Assert.assertNotNull(ptuSettlementXml);
        Assert.assertEquals(BigInteger.valueOf(2000), ptuSettlementXml.getActualPower());
        Assert.assertEquals(BigInteger.valueOf(1000), ptuSettlementXml.getDeliveredFlexPower());
        Assert.assertEquals(BigInteger.valueOf(1), ptuSettlementXml.getDuration());
        Assert.assertEquals(BigInteger.valueOf(2000), ptuSettlementXml.getOrderedFlexPower());
        Assert.assertEquals(BigInteger.valueOf(1000), ptuSettlementXml.getPowerDeficiency());
        Assert.assertEquals(BigInteger.valueOf(1000), ptuSettlementXml.getPrognosisPower());
        Assert.assertEquals(BigInteger.valueOf(48), ptuSettlementXml.getStart());
        Assert.assertEquals(BigDecimal.valueOf(30), ptuSettlementXml.getNetSettlement());
        Assert.assertEquals(BigDecimal.valueOf(20), ptuSettlementXml.getPenalty());
        Assert.assertEquals(BigDecimal.valueOf(50), ptuSettlementXml.getPrice());
    }


    @Test
    public void testMapXmlToDto() {
        FlexOrderSettlementDto flexOrderSettlementDto = SettlementTransformer.mapXmlToDto(buildFlexOrderSettlementXml(),
                "agr.usef-example.com");
        Assert.assertEquals(1L, flexOrderSettlementDto.getFlexOrder().getSequenceNumber().longValue());
        Assert.assertEquals(PERIOD, flexOrderSettlementDto.getPeriod());
        Assert.assertEquals(96, flexOrderSettlementDto.getPtuSettlementDtos().size());

    }

    @Test
    public void testMapPtuXmlToDto(){
        PTUSettlement ptuSettlement = new PTUSettlement();
        ptuSettlement.setStart(BigInteger.valueOf(1));
        ptuSettlement.setDuration(BigInteger.valueOf(96));
        ptuSettlement.setActualPower(BigInteger.valueOf(1000L));
        ptuSettlement.setDeliveredFlexPower(BigInteger.valueOf(1000L));
        ptuSettlement.setOrderedFlexPower(BigInteger.valueOf(1000L));
        ptuSettlement.setPrognosisPower(BigInteger.valueOf(1000L));
        ptuSettlement.setPowerDeficiency(BigInteger.valueOf(1000L));
        ptuSettlement.setPrice(BigDecimal.valueOf(1000L));
        ptuSettlement.setPenalty(BigDecimal.valueOf(100L));
        ptuSettlement.setNetSettlement(BigDecimal.valueOf(900L));
        List<PtuSettlementDto> ptuSettlementDtos = SettlementTransformer.transformPtuXmlToDto(ptuSettlement);
        Assert.assertEquals(96, ptuSettlementDtos.size());
        Assert.assertEquals(BigInteger.valueOf(1000L), ptuSettlementDtos.get(0).getActualPower());
        Assert.assertEquals(BigInteger.valueOf(1000L), ptuSettlementDtos.get(0).getDeliveredFlexPower());
        Assert.assertEquals(BigInteger.valueOf(1000L), ptuSettlementDtos.get(0).getPrognosisPower());
        Assert.assertEquals(BigInteger.valueOf(1000L), ptuSettlementDtos.get(0).getOrderedFlexPower());
        Assert.assertEquals(BigInteger.valueOf(1000L), ptuSettlementDtos.get(0).getPowerDeficiency());
        Assert.assertEquals(BigInteger.valueOf(1000L), ptuSettlementDtos.get(0).getActualPower());
        Assert.assertEquals(BigDecimal.valueOf(1000L), ptuSettlementDtos.get(0).getPrice());
        Assert.assertEquals(BigDecimal.valueOf(100L), ptuSettlementDtos.get(0).getPenalty());
        Assert.assertEquals(BigDecimal.valueOf(900L), ptuSettlementDtos.get(0).getNetSettlement());
        Assert.assertEquals(BigInteger.valueOf(1), ptuSettlementDtos.get(0).getPtuIndex());
    }

    @Test
    public void testMapModelToDto() {
        energy.usef.core.model.FlexOrderSettlement flexOrderSettlement = new energy.usef.core.model.FlexOrderSettlement();
        flexOrderSettlement.setConnectionGroup(CONNECTION_GROUP);
        flexOrderSettlement.setPeriod(PERIOD);
        PlanboardMessage flexOrder = new PlanboardMessage();
        flexOrder.setSequence(1L);
        flexOrder.setParticipantDomain("agr.usef-example.com");
        flexOrder.setConnectionGroup(CONNECTION_GROUP);
        flexOrderSettlement.setFlexOrder(flexOrder);
        FlexOrderSettlementDto flexOrderSettlementDto = SettlementTransformer.mapModelToDto(flexOrderSettlement);
        Assert.assertNotNull(flexOrderSettlementDto);
        Assert.assertEquals(PERIOD, flexOrderSettlementDto.getPeriod());
        Assert.assertEquals(1L, flexOrderSettlementDto.getFlexOrder().getSequenceNumber().longValue());
        Assert.assertEquals("agr.usef-example.com", flexOrderSettlementDto.getFlexOrder().getParticipantDomain());
        Assert.assertEquals(CONNECTION_GROUP.getUsefIdentifier(), flexOrderSettlementDto.getFlexOrder().getConnectionGroupEntityAddress());
    }

    FlexOrderSettlement buildFlexOrderSettlementXml() {
        FlexOrderSettlement fos = new FlexOrderSettlement();
        fos.setPeriod(PERIOD);
        fos.setCongestionPoint(CONNECTION_GROUP.getUsefIdentifier());
        fos.setOrderReference("1");
        PTUSettlement ptuSettlement = new PTUSettlement();
        ptuSettlement.setStart(BigInteger.valueOf(1));
        ptuSettlement.setDuration(BigInteger.valueOf(96));
        fos.getPTUSettlement().add(ptuSettlement);
        return fos;
    }
}
