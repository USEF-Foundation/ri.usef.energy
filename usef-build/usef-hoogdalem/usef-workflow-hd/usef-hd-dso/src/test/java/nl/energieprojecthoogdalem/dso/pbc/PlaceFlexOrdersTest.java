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

package nl.energieprojecthoogdalem.dso.pbc;

import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.FlexOfferDto;
import info.usef.core.workflow.dto.PtuFlexOfferDto;
import info.usef.dso.workflow.validate.create.flexorder.PlaceFlexOrdersStepParameter.IN;
import info.usef.dso.workflow.validate.create.flexorder.PlaceFlexOrdersStepParameter.OUT;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class in charge of the unit tests related to the {@link PlaceFlexOrders} class.
 */
@RunWith(PowerMockRunner.class)
public class PlaceFlexOrdersTest
{
    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.11112222";
    private static final int PTUS_PER_DAY = 96;

    private PlaceFlexOrders dsoPlaceFlexOrders;

    @SuppressWarnings("unchecked") @Test
    public void testInvokeReturnsAcceptedOffers()
    {
        dsoPlaceFlexOrders = new PlaceFlexOrders();


        WorkflowContext context = buildWorkflowContext();
        context = dsoPlaceFlexOrders.invoke(context);

        List<Long> offerSequences = context.get(OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), List.class);

        Assert.assertNotNull(offerSequences);
        Assert.assertTrue(!offerSequences.isEmpty());

        Assert.assertEquals(3, offerSequences.size());
    }

    private WorkflowContext buildWorkflowContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), CONGESTION_POINT_ENTITY_ADDRESS);
        context.setValue(IN.PERIOD.name(), new LocalDate());
        context.setValue(IN.PTU_DURATION.name(), 15);

        // create empty flexOfferDto list with 3 items
        List<FlexOfferDto> flexOfferList = buildFlexOffersList();

        // fill in some power and price values
        flexOfferList.get(0).getPtus().get(1).setPower(BigInteger.valueOf(1000));
        flexOfferList.get(0).getPtus().get(1).setPrice(new BigDecimal(1));
        flexOfferList.get(0).getPtus().get(2).setPower(BigInteger.valueOf(10000));
        flexOfferList.get(0).getPtus().get(2).setPrice(new BigDecimal(10));

        flexOfferList.get(1).getPtus().get(1).setPower(BigInteger.valueOf(1000));
        flexOfferList.get(1).getPtus().get(1).setPrice(new BigDecimal(-1));
        flexOfferList.get(1).getPtus().get(2).setPower(BigInteger.valueOf(10000));
        flexOfferList.get(1).getPtus().get(2).setPrice(new BigDecimal(-10));

        flexOfferList.get(2).getPtus().get(94).setPower(BigInteger.valueOf(1000));
        flexOfferList.get(2).getPtus().get(94).setPrice(new BigDecimal(1));
        flexOfferList.get(2).getPtus().get(95).setPower(BigInteger.valueOf(10000));
        flexOfferList.get(2).getPtus().get(95).setPrice(new BigDecimal(10));

        context.setValue(IN.FLEX_OFFER_DTO_LIST.name(), flexOfferList);

        return context;
    }

    private List<FlexOfferDto> buildFlexOffersList()
    {
        return IntStream.rangeClosed(1, 3).mapToObj(this::createFlexOffer).collect(Collectors.toList());
    }

    private FlexOfferDto createFlexOffer(int sequence)
    {
        FlexOfferDto offer = new FlexOfferDto();
        offer.setSequenceNumber((long) sequence);
        offer.setPeriod(new LocalDate());
        offer.setPtus(buildPtuFlexOffers());
        return offer;
    }

    private List<PtuFlexOfferDto> buildPtuFlexOffers()
    {
        return IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(this::createPtuFlexOffer).collect(Collectors.toList());
    }

    private PtuFlexOfferDto createPtuFlexOffer(int ptuIndex)
    {
        PtuFlexOfferDto ptuFlexOfferDto = new PtuFlexOfferDto();
        ptuFlexOfferDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
        ptuFlexOfferDto.setPower(BigInteger.ZERO);
        ptuFlexOfferDto.setPrice(BigDecimal.ZERO);

        return ptuFlexOfferDto;
    }
}
