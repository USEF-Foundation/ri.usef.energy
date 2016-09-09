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

package energy.usef.brp.workflow.altstep;

import energy.usef.brp.workflow.plan.flexorder.place.PlaceFlexOrdersStepParameter;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.FlexOfferDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Workflow step implementation for the Workflow 'BRP Place Flex Orders'.
 * This stub orders all flex offers.
 *
 * This implementation expects to find the following parameters as input:
 * <ul>
 * <li>CONNECTION_GROUP_IDENTIFIER ({@link String}): usef identifier of the connection group.</li>
 * <li>PTU_DURATION ({@link Integer}): duration of one ptu in minutes.</li>
 * <li>FLEX_OFFER_DTO_LIST ({@link List} of {@link FlexOfferDto}): the flex offers which can be
 * accepted. When the flex offer is not accepted, the next time the step is called, the flex offer is offered again.</li>
 * </ul>
 * <p/>
 * parameters as output:
 * <ul>
 * <li>ACCEPTED_FLEX_OFFER_SEQUENCE_LIST: the sequence number of flex offers which are accepted by the BRP.</li>
 * </ul>
 */
public class BrpPlaceFlexOrdersStubOrder implements WorkflowStep {
    // Adjusted to always Order

    private static final Logger LOGGER = LoggerFactory.getLogger(BrpPlaceFlexOrdersStubOrder.class);

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {

        LOGGER.debug("Starting workflow step 'BRP Place Flex Orders' with parameters given in the context: {}", context);

        // get the input parameters for this PBC
        List<FlexOfferDto> offers = context.get(PlaceFlexOrdersStepParameter.IN.FLEX_OFFER_DTO_LIST.name(), List.class);
        Integer ptuDuration = context.get(PlaceFlexOrdersStepParameter.IN.PTU_DURATION.name(), Integer.class);

        List<Long> acceptedOffers = new ArrayList<>();

        if (!offers.isEmpty()) {

            // Accept all offers.
            List<FlexOfferDto> acceptedOffersList = offers.stream().collect(Collectors.toList());

            if (acceptedOffersList.isEmpty()) {
                LOGGER.debug("No flex offers are accepted, so no flex orders will be created.");
            } else {
                LOGGER.debug("Found {} flex offers that within the acceptable price range.", acceptedOffersList.size());

                acceptedOffersList.forEach(offer -> acceptedOffers.add(offer.getSequenceNumber()));
            }

        }

        context.setValue(PlaceFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), acceptedOffers);

        LOGGER.debug("Ending successfully workflow step 'BRP Place Flex Orders'.");
        return context;
    }

}
