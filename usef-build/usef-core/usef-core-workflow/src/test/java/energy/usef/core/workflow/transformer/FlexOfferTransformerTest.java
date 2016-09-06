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

import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests relates to the {@link FlexOfferTransformer} class.
 */
public class FlexOfferTransformerTest {

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, FlexOfferTransformer.class.getDeclaredConstructors().length);
        Constructor<FlexOfferTransformer> constructor = FlexOfferTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransformPlanboardMessage() {
        LocalDateTime now = DateTimeUtil.getCurrentDateTime();

        PlanboardMessage flexOffer = new PlanboardMessage();
        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier("ean.012345678901234567");
        flexOffer.setConnectionGroup(connectionGroup);
        flexOffer.setOriginSequence(1L);
        flexOffer.setSequence(2L);
        flexOffer.setParticipantDomain("agr.usef-example.com");
        flexOffer.setPeriod(DateTimeUtil.parseDate("2015-02-02"));
        flexOffer.setExpirationDate(now);
        FlexOfferDto flexOfferDto = FlexOfferTransformer.transform(flexOffer);
        Assert.assertNotNull(flexOfferDto);
        Assert.assertEquals("ean.012345678901234567", flexOfferDto.getConnectionGroupEntityAddress());
        Assert.assertEquals("agr.usef-example.com", flexOfferDto.getParticipantDomain());
        Assert.assertEquals(1L, flexOfferDto.getFlexRequestSequenceNumber().longValue());
        Assert.assertEquals(DateTimeUtil.parseDate("2015-02-02"), flexOfferDto.getPeriod());
        Assert.assertEquals(2L, flexOfferDto.getSequenceNumber().longValue());
        Assert.assertEquals(now, flexOfferDto.getExpirationDateTime());

        Assert.assertNull(FlexOfferTransformer.transform(null));
    }

    @Test
    public void testTransformPtuFlexOffer() {
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuDate(DateTimeUtil.parseDate("2015-02-02"));
        ptuContainer.setPtuIndex(9);
        PtuFlexOffer ptuFlexOffer = new PtuFlexOffer();
        ptuFlexOffer.setPower(BigInteger.valueOf(999));
        ptuFlexOffer.setPrice(BigDecimal.valueOf(9.99d));
        ptuFlexOffer.setPtuContainer(ptuContainer);
        PtuFlexOfferDto dto = FlexOfferTransformer.transformPtuFlexOffer(ptuFlexOffer);
        Assert.assertNotNull(dto);
        Assert.assertEquals(BigInteger.valueOf(999), dto.getPower());
        Assert.assertEquals(BigDecimal.valueOf(9.99), dto.getPrice());
        Assert.assertEquals(BigInteger.valueOf(9), dto.getPtuIndex());

        Assert.assertNull(FlexOfferTransformer.transformPtuFlexOffer(null));
    }

    @Test
    public void testTransformPtuFlexOffers() {
        FlexOfferDto flexOfferDto = FlexOfferTransformer.transformPtuFlexOffers(buildPtuFlexOffers());
        Assert.assertNotNull(flexOfferDto);
        Assert.assertEquals("brp.usef-example.com", flexOfferDto.getParticipantDomain());
        Assert.assertEquals(2l, flexOfferDto.getFlexRequestSequenceNumber().longValue());
        Assert.assertEquals(3l, flexOfferDto.getSequenceNumber().longValue());
        Assert.assertEquals("brp.usef-example.com", flexOfferDto.getConnectionGroupEntityAddress());
        Assert.assertEquals(new LocalDate(2015, 2, 2), flexOfferDto.getPeriod());
        Assert.assertEquals(96, flexOfferDto.getPtus().size());

        Assert.assertNull(FlexOfferTransformer.transformPtuFlexOffers(null));
    }

    @Test
    public void testTransformToPtu() {
        PtuFlexOfferDto ptuFlexOffer = new PtuFlexOfferDto();
        ptuFlexOffer.setPower(BigInteger.TEN);
        ptuFlexOffer.setPrice(BigDecimal.ONE);
        ptuFlexOffer.setPtuIndex(BigInteger.valueOf(2));
        PTU ptu = FlexOfferTransformer.transformToPTU(ptuFlexOffer);
        Assert.assertNotNull(ptu);
        Assert.assertNull(ptu.getDisposition());
        Assert.assertEquals(1, ptu.getDuration().intValue());
        Assert.assertEquals(BigInteger.TEN, ptu.getPower());
        Assert.assertEquals(BigDecimal.valueOf(1), ptu.getPrice());
        Assert.assertEquals(BigInteger.valueOf(2), ptu.getStart());

        Assert.assertNull(FlexOfferTransformer.transformToPTU(null));    }

    private List<PtuFlexOffer> buildPtuFlexOffers() {
        BrpConnectionGroup connectionGroup = new BrpConnectionGroup();
        connectionGroup.setUsefIdentifier("brp.usef-example.com");
        return IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setPtuDate(DateTimeUtil.parseDate("2015-02-02"));
            ptuContainer.setPtuIndex(index);
            PtuFlexOffer ptuFlexOffer = new PtuFlexOffer();
            ptuFlexOffer.setPtuContainer(ptuContainer);
            ptuFlexOffer.setConnectionGroup(connectionGroup);
            ptuFlexOffer.setPower(BigInteger.TEN);
            ptuFlexOffer.setPrice(BigDecimal.ONE);
            ptuFlexOffer.setParticipantDomain("brp.usef-example.com");
            ptuFlexOffer.setSequence(3l);
            ptuFlexOffer.setFlexRequestSequence(2l);
            return ptuFlexOffer;
        }).collect(Collectors.toList());
    }
}
