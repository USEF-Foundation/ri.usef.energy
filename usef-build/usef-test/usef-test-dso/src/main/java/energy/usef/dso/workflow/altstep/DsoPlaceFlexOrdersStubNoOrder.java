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

package energy.usef.dso.workflow.altstep;

import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;
import energy.usef.dso.pbcfeederimpl.PbcFeederService;
import energy.usef.dso.workflow.validate.create.flexorder.PlaceFlexOrdersStepParameter.IN;
import energy.usef.dso.workflow.validate.create.flexorder.PlaceFlexOrdersStepParameter.OUT;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Workflow step implementation for the Workflow 'DSO Place Flex Orders'.
 * This stub does not order any offer.
 *
 * This implementation expects to find the following parameters as input:
 * <ul>
 * <li>FLEX_OFFER_DTO_LIST ({@link List<FlexOfferDto>}): Flex offer DTO list.</li>
 * <li>CONGESTION_POINT_ENTITY_ADDRESS ({@link String}): Entity address of the congestion point.</li>
 * <li>PTU_DURATION ({@link Integer}): duration of one ptu in minutes.</li>
 * <li>PERIOD ({@link LocalDate}): date of the flex offers.</li>
 * * </ul>
 * <p>
 * parameters as output:
 * <ul>
 * <li>ACCEPTED_FLEX_OFFER_SEQUENCE_LIST: Sequence numbers of the accepted flex offers.</li>
 * </ul>
 */
public class DsoPlaceFlexOrdersStubNoOrder implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoPlaceFlexOrdersStubNoOrder.class);

    @Inject
    private PbcFeederService pbcFeederService;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {

        LOGGER.debug("Starting workflow step 'DSO Place Flex Orders' with parameters given in the context: {}", context);

        // get the input parameters for this PBC
        List<FlexOfferDto> offers = context.get(IN.FLEX_OFFER_DTO_LIST.name(), List.class);
        String congestionPoint = context.get(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), String.class);
        Integer ptuDuration = context.get(IN.PTU_DURATION.name(), Integer.class);
        LocalDate period = context.get(IN.PERIOD.name(), LocalDate.class);

        if (offers.isEmpty()) {
            context.setValue(OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), new ArrayList<>());
            LOGGER.debug("Ending workflow step 'DSO Place Flex Orders' due to empty offer list.");
            return context;
        }

        List<Long> acceptedOffers = new ArrayList<>();

        // retrieve the data from the PBC Feeder
        Map<Integer, BigDecimal> pbcStubData = pbcFeederService
                .retrieveApxPrices(period, 1, PtuUtil.getNumberOfPtusPerDay(period, ptuDuration));

        LOGGER.debug("No flex offers are accepted, so no flex orders will be created.");

        context.setValue(OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), acceptedOffers);

        LOGGER.debug("Ending successfully workflow step 'DSO Place Flex Orders'.");
        return context;
    }
}
