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
import energy.usef.brp.workflow.plan.flexorder.place.PlaceFlexOrdersStepParameter;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;

import java.math.BigDecimal;
import java.math.BigInteger;
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
 * Test class in charge of the unit tests related to the {@link BrpPlaceFlexOrdersStub} class.
 */
@RunWith(PowerMockRunner.class)
public class BrpPlaceFlexOrdersStubTest {

    private static final String CONNECTION_GROUP_IDENTIFIER = "agr.usef-example.com";
    private static final int PTUS_PER_DAY = 96;
    @Mock
    PbcFeederService pbcFeederService;
    private BrpPlaceFlexOrdersStub brpPlaceOperateFlexOrders;

    @Before
    public void init() throws Exception {
        brpPlaceOperateFlexOrders = new BrpPlaceFlexOrdersStub();

        Whitebox.setInternalState(brpPlaceOperateFlexOrders, pbcFeederService);

        // pbc feeder will return some values (-470 (ptu index 1) to +480 (ptu index 96) in steps of 10 per ptu)
        Mockito.when(pbcFeederService.retrieveApxPrices(Mockito.any(), Mockito.anyInt(), Mockito.anyInt()))
                .then(call -> IntStream.rangeClosed(1, PTUS_PER_DAY)
                        .mapToObj(Integer::valueOf)
                        .collect(Collectors.toMap(Function.identity(), i -> new BigDecimal((i * 10) - PTUS_PER_DAY / 2 * 10))));
    }

    @SuppressWarnings("unchecked") @Test
    public void testInvokeReturnsAcceptedOffers() {
        WorkflowContext context = buildWorkflowContext();
        context = brpPlaceOperateFlexOrders.invoke(context);

        List<Long> offerSequences = context
                .get(PlaceFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), List.class);

        Assert.assertNotNull(offerSequences);
        Assert.assertTrue(!offerSequences.isEmpty());
        // Only one flex offers will be accepted, the prices of the other ptu flex offers are too high or too low
        Assert.assertEquals(1, offerSequences.size());
        Assert.assertEquals(2l, offerSequences.get(0).longValue());
    }

    private WorkflowContext buildWorkflowContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(PlaceFlexOrdersStepParameter.IN.CONNECTION_GROUP_IDENTIFIER.name(), CONNECTION_GROUP_IDENTIFIER);
        context.setValue(PlaceFlexOrdersStepParameter.IN.PTU_DURATION.name(), 15);
        context.setValue(PlaceFlexOrdersStepParameter.IN.PERIOD.name(), new LocalDate());

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

        context.setValue(PlaceFlexOrdersStepParameter.IN.FLEX_OFFER_DTO_LIST.name(), flexOfferList);

        return context;
    }

    private List<FlexOfferDto> buildFlexOffersList() {
        return IntStream.rangeClosed(1, 3).mapToObj(this::createFlexOffer).collect(Collectors.toList());
    }

    private FlexOfferDto createFlexOffer(int sequence) {
        FlexOfferDto offer = new FlexOfferDto();
        offer.setSequenceNumber((long) sequence);
        offer.setPeriod(new LocalDate());
        offer.setPtus(buildPtuFlexOffers());
        return offer;
    }

    private List<PtuFlexOfferDto> buildPtuFlexOffers() {
        return IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(this::createPtuFlexOffer).collect(Collectors.toList());
    }

    private PtuFlexOfferDto createPtuFlexOffer(int ptuIndex) {
        PtuFlexOfferDto ptuFlexOfferDto = new PtuFlexOfferDto();
        ptuFlexOfferDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
        ptuFlexOfferDto.setPower(BigInteger.ZERO);
        ptuFlexOfferDto.setPrice(BigDecimal.ZERO);

        return ptuFlexOfferDto;
    }
}
