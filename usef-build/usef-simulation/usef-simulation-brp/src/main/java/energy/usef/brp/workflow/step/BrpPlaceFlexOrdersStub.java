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
import energy.usef.core.util.PowerUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow step implementation for the Workflow 'BRP Place Flex Orders'. This implementation expects to find the following
 * parameters as input:
 * <ul>
 * <li>CONNECTION_GROUP_IDENTIFIER ({@link String}): usef identifier of the connection group.</li>
 * <li>PTU_DURATION ({@link Integer}): duration of one ptu in minutes.</li>
 * <li>PERIOD ({@link LocalDate}): period of the flex offers.</li>
 * * <li>FLEX_OFFER_DTO_LIST ({@link List} of {@link FlexOfferDto}): the flex offers which can be
 * accepted. When the flex offer is not accepted, the next time the step is called, the flex offer is offered again.</li>
 * </ul>
 * <p>
 * Offers are accepted when all ptuFlexOffers are below {ACCEPTANCE_THRESHOLD_NEGATIVE_APX} of the apx prices for negative
 * apx prices and below {ACCEPTANCE_THRESHOLD_POSITIVE_APX} of the apx price for positive apx prices.
 * </p>
 * <p>
 * parameters as output:
 * <ul>
 * <li>ACCEPTED_FLEX_OFFER_SEQUENCE_LIST: the sequence number of flex offers which are accepted by the BRP.</li>
 * </ul>
 */
public class BrpPlaceFlexOrdersStub implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrpPlaceFlexOrdersStub.class);

    private static final double ACCEPTANCE_THRESHOLD_NEGATIVE_APX = 1.54;
    private static final double ACCEPTANCE_THRESHOLD_POSITIVE_APX = 0.65;

    @Inject
    private PbcFeederService pbcFeederService;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {

        LOGGER.debug("Starting workflow step 'BRP Place Flex Orders' with parameters given in the context: {}", context);

        // get the input parameters for this PBC
        String usefIdentifier = context.get(PlaceFlexOrdersStepParameter.IN.CONNECTION_GROUP_IDENTIFIER.name(), String.class);
        List<FlexOfferDto> offers = context.get(PlaceFlexOrdersStepParameter.IN.FLEX_OFFER_DTO_LIST.name(), List.class);
        Integer ptuDuration = context.get(PlaceFlexOrdersStepParameter.IN.PTU_DURATION.name(), Integer.class);
        LocalDate period = context.get(PlaceFlexOrdersStepParameter.IN.PERIOD.name(), LocalDate.class);

        if (offers.isEmpty()) {
            context.setValue(PlaceFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), new ArrayList<>());
            LOGGER.debug("Ending workflow step 'BRP Place Flex Orders' due to empty offer list.");
            return context;
        }

        List<Long> acceptedOffers = new ArrayList<>();

        // retrieve the data from the PBC Feeder
        Map<Integer, BigDecimal> pbcStubData = pbcFeederService
                .retrieveApxPrices(period, 1, PtuUtil.getNumberOfPtusPerDay(period, ptuDuration));

        // only accept offers with ptuFlexOffers that ALL are between the apx price thresholds.
        offers.stream().filter(flexOffer -> hasPtuFlexOffersWithAllPricesBetweenThresholds(flexOffer, pbcStubData, ptuDuration)).
                collect(Collectors.toList()).forEach(offer -> acceptedOffers.add(offer.getSequenceNumber()));

        if (acceptedOffers.isEmpty()) {
            LOGGER.debug("No flex offers are accepted, so no flex orders will be created.");
        } else {
            LOGGER.debug("Found {} flex offers that are within the acceptable price range.", acceptedOffers.size());
        }

        context.setValue(PlaceFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), acceptedOffers);

        LOGGER.debug("Ending successfully workflow step 'BRP Place Flex Orders'.");
        return context;
    }

    /**
     * This method will return true if all prices of all ({@link PtuFlexOfferDto)'s are below the threshold.
     *
     * @param flexOffer
     * @param pbcStubData
     * @param ptuDuration
     * @return true if all ptuFlexOfferDto's are accepted
     */
    private boolean hasPtuFlexOffersWithAllPricesBetweenThresholds(FlexOfferDto flexOffer, Map<Integer, BigDecimal> pbcStubData,
            int ptuDuration) {

        for (PtuFlexOfferDto ptuFlexOffer : flexOffer.getPtus()) {
            BigDecimal apxPrice = pbcStubData.get(ptuFlexOffer.getPtuIndex().intValue());
            if (apxPrice == null) {
                LOGGER.error("No apx price available for ptu {}", ptuFlexOffer.getPtuIndex());
                return false;
            }

            // convert flex offer power and price to price per MWH (in order to compare it with the apx prices)
            BigDecimal offerPrice = PowerUtil
                    .wattPricePerPTUToMWhPrice(ptuFlexOffer.getPower(), ptuFlexOffer.getPrice(), ptuDuration);

            // only check for offer MWH prices and power other than 0
            if (offerPrice.compareTo(BigDecimal.ZERO) == 0 && ptuFlexOffer.getPower().compareTo(BigInteger.ZERO) == 0) {
                continue;
            }

            if (!isPriceWithinThreshold(flexOffer, ptuFlexOffer, apxPrice, offerPrice)) {
                return false;
            }
        }

        return true;
    }

    private boolean isPriceWithinThreshold(FlexOfferDto flexOffer, PtuFlexOfferDto ptuFlexOffer, BigDecimal apxPrice,
            BigDecimal offerPrice) {
        if (apxPrice.compareTo(BigDecimal.ZERO) < 0) {
            // apx price is less than zero
            BigDecimal thresholdPrice = apxPrice.multiply(BigDecimal.valueOf(ACCEPTANCE_THRESHOLD_NEGATIVE_APX));

            if (offerPrice.compareTo(thresholdPrice) >= 0) {
                LOGGER.debug("Price of PtuFlexOfferDto (ptu {}, price {} (per MWh)) > threshold price {} (apx price {}), "
                        + "flexOffer {} is not accepted.", ptuFlexOffer.getPtuIndex(), offerPrice, thresholdPrice, apxPrice,
                        flexOffer.getSequenceNumber());
                return false;
            }
        } else {
            // apx price is more than or equal to zero
            BigDecimal thresholdPrice = apxPrice.multiply(BigDecimal.valueOf(ACCEPTANCE_THRESHOLD_POSITIVE_APX));

            if (offerPrice.compareTo(thresholdPrice) >= 0) {
                LOGGER.debug("Price of PtuFlexOfferDto (ptu {}, price {} (per MWh)) > threshold price {} (apx price {}), "
                        + "flexOffer {} is not accepted.", ptuFlexOffer.getPtuIndex(), offerPrice, thresholdPrice, apxPrice,
                        flexOffer.getSequenceNumber());
                return false;
            }
        }
        return true;
    }
}
