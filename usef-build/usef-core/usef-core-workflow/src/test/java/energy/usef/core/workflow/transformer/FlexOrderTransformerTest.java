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
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.dto.AcknowledgementStatusDto;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link FlexOrderTransformer} class.
 */
public class FlexOrderTransformerTest {

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, FlexOrderTransformer.class.getDeclaredConstructors().length);
        Constructor<FlexOrderTransformer> constructor = FlexOrderTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);

        Assert.assertNull(FlexOrderTransformer.transform(null));
    }

    @Test
    public void testTransformPtuFlexOrders() throws Exception {
        FlexOrderDto flexOrderDto = FlexOrderTransformer.transformPtuFlexOrders(buildPtuFlexOrders());
        Assert.assertNotNull(flexOrderDto);
        Assert.assertEquals("ean.123456789012345678", flexOrderDto.getConnectionGroupEntityAddress());
        Assert.assertEquals(AcknowledgementStatusDto.ACCEPTED, flexOrderDto.getAcknowledgementStatus());
        Assert.assertEquals(4l, flexOrderDto.getSequenceNumber().longValue());
        Assert.assertEquals(3l, flexOrderDto.getFlexOfferSequenceNumber().longValue());
        Assert.assertEquals("dso.usef-example.com", flexOrderDto.getParticipantDomain());
    }

    @Test
    public void testTransformPtuFlexOrderDtoToPtuIsSuccessful() {
        PtuFlexOrderDto ptuFlexOrderDto = new PtuFlexOrderDto();
        ptuFlexOrderDto.setPrice(BigDecimal.valueOf(9.99));
        ptuFlexOrderDto.setPtuIndex(BigInteger.valueOf(9));
        ptuFlexOrderDto.setPower(BigInteger.valueOf(999));
        PTU ptu = FlexOrderTransformer.transformPtuFlexOrderDtoToPtu(ptuFlexOrderDto);
        Assert.assertNotNull(ptu);
        Assert.assertEquals(BigInteger.ONE, ptu.getDuration());
        Assert.assertEquals(BigInteger.valueOf(9), ptu.getStart());
        Assert.assertEquals(BigInteger.valueOf(999), ptu.getPower());
        Assert.assertEquals(BigDecimal.valueOf(9.99), ptu.getPrice());
        Assert.assertNull(ptu.getDisposition());

        Assert.assertNull(FlexOrderTransformer.transformPtuFlexOrderDtoToPtu(null));
    }

    private List<PtuFlexOrder> buildPtuFlexOrders() {
        final LocalDate ptuDate = DateTimeUtil.parseDate("2015-02-02");
        final CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setUsefIdentifier("ean.123456789012345678");
        return IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setPtuIndex(index);
            ptuContainer.setPtuDate(ptuDate);

            PtuFlexOrder ptuFlexOrder = new PtuFlexOrder();
            ptuFlexOrder.setPtuContainer(ptuContainer);
            ptuFlexOrder.setAcknowledgementStatus(AcknowledgementStatus.ACCEPTED);
            ptuFlexOrder.setParticipantDomain("dso.usef-example.com");
            ptuFlexOrder.setSequence(4l);
            ptuFlexOrder.setFlexOfferSequence(3l);
            ptuFlexOrder.setConnectionGroup(congestionPoint);
            return ptuFlexOrder;
        }).collect(Collectors.toList());
    }
}
