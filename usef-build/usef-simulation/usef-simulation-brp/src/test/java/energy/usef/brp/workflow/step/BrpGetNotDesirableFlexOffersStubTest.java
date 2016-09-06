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

import energy.usef.brp.workflow.plan.flexorder.place.GetNotDesirableFlexOffersParameter;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOfferDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link BrpGetNotDesirableFlexOffersStub} class.
 */
public class BrpGetNotDesirableFlexOffersStubTest {
    private static final String CONNECTION_GROUP_IDENTIFIER = "agr.usef-example.com";

    private BrpGetNotDesirableFlexOffersStub brpGetNotDesirableFlexOffersStub;

    @Before
    public void init() {
        brpGetNotDesirableFlexOffersStub = new BrpGetNotDesirableFlexOffersStub();
    }

    @Test
    public void testInvoke() {
        WorkflowContext context = buildWorkflowContext();
        context = brpGetNotDesirableFlexOffersStub.invoke(context);

        @SuppressWarnings("unchecked")
        List<Long> notDesirableFlexOfferSequences = (List<Long>) context
                .getValue(GetNotDesirableFlexOffersParameter.OUT.NOT_DESIRABLE_FLEX_OFFER_SEQUENCE_LIST
                        .name());
        Assert.assertNotNull(notDesirableFlexOfferSequences);
    }

    private WorkflowContext buildWorkflowContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(GetNotDesirableFlexOffersParameter.IN.CONNECTION_GROUP_IDENTIFIER.name(), CONNECTION_GROUP_IDENTIFIER);
        context.setValue(GetNotDesirableFlexOffersParameter.IN.FLEX_OFFER_DTO_LIST.name(), buildFlexOffersList());
        return context;
    }

    private List<FlexOfferDto> buildFlexOffersList() {
        return IntStream.rangeClosed(1, 4).mapToObj(this::createFlexOffer).collect(Collectors.toList());
    }

    private FlexOfferDto createFlexOffer(int sequence) {
        FlexOfferDto offer = new FlexOfferDto();
        offer.setSequenceNumber((long) sequence);
        offer.setPtus(new ArrayList<>());
        return offer;
    }

}
