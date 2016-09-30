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

import energy.usef.brp.workflow.plan.flexorder.place.GetNotDesirableFlexOffersParameter;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.FlexOfferDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow step implementation for the 'Get Not Desirable Flex Offers' step of the 'Place Flex Orders' Workflow.
 * This stub states that all flex offers are desirable.
 *
 * This implementation expects to find the following parameters as input:
 * <ul>
 * <li>CONNECTION_GROUP_IDENTIFIER ({@link String}): USEF identifier of the connection group.</li>
 * <li>FLEX_OFFER_DTO_LIST ({@link List} of {@link FlexOfferDto}): Flex offer DTO list.</li>
 * </ul>
 * <p/>
 * parameters as output:
 * <ul>
 * <li>NOT_DESIRABLE_FLEX_OFFER_SEQUENCE_LIST: the sequence number of flex offers which are not desirable.</li>
 * </ul>
 */
public class BrpGetNotDesirableFlexOffersStubAccept implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrpGetNotDesirableFlexOffersStubAccept.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.debug("Parameters given in the context:\n # Usef identifier: {},\n # FlexOffers: {}",
                context.getValue(GetNotDesirableFlexOffersParameter.IN.CONNECTION_GROUP_IDENTIFIER.name()),
                context.getValue(GetNotDesirableFlexOffersParameter.IN.FLEX_OFFER_DTO_LIST.name()));

        context.setValue(GetNotDesirableFlexOffersParameter.OUT.NOT_DESIRABLE_FLEX_OFFER_SEQUENCE_LIST.name(), new ArrayList<>());
        LOGGER.debug("Ending successfully workflow step 'Place Flex Orders'.");
        return context;
    }

}
