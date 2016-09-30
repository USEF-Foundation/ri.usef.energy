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

package energy.usef.agr.workflow.altstep;

//import PbcFeederService;

import energy.usef.agr.workflow.validate.flexoffer.FlexOfferDetermineFlexibilityStepParameter;
import energy.usef.agr.workflow.validate.flexoffer.FlexOfferDetermineFlexibilityStepParameter.IN;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.*;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementation of a workflow step "AGRFlexOfferDetermineFlexibility".
 * <p>
 * The PBC receives the following parameters as input:
 * <ul>
 * <li>PTU_DAY: PTU day {@link org.joda.time.LocalDate}.</li>
 * <li>LATEST_D_PROGNOSES_DTO_LIST: {@link List} of {@link PrognosisDto}.</li>
 * <li>LATEST_A_PLANS_DTO_LIST : the list of the latest A-Plans {@link PrognosisDto} of the
 * 'A-Plan'type.</li>
 * </ul>
 * <p>
 * The PBC returns the following parameters as output:
 * <ul>
 * <li>FLEX_OFFERS_DTO_LIST : Flex offer DTO list {@link List} of {@link FlexOfferDto}.</li>
 * </ul>
 * <p>
 * This step never generates a flex offer.
 */
public class AgrFlexOfferDetermineFlexibilityStubNoOffer implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrFlexOfferDetermineFlexibilityStubNoOffer.class);
    private static final int FLEX_OFFER_EXPIRATION_DAYS = 3;
    //private static final Random RANDOM = new Random();
    LocalDateTime now = DateTimeUtil.getCurrentDateTime();

    @Inject
    //private PbcFeederService pbcFeederService;

    /*
     * (non-Javadoc)
     * 
     * @see WorkflowStep#invoke(WorkflowContext)
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("AgrFlexOfferStub: started");
        List<FlexOfferDto> outputFlexOffers = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<FlexRequestDto> inputFlexRequests = (List<FlexRequestDto>) context.getValue(IN.FLEX_REQUEST_DTO_LIST.name());

        /*
         * For each give flex request, choose to: - create an offer, - do not create offer at all, - do not create offer now.
         */
        int decision;
        for (FlexRequestDto flexRequestDto : inputFlexRequests) {
            decision = 2;//RANDOM.nextInt(3); // 0,1 or 2
            FlexOfferDto flexOfferDto = new FlexOfferDto();
            flexOfferDto.setFlexRequestSequenceNumber(flexRequestDto.getSequenceNumber());
            flexOfferDto.setExpirationDateTime(now.plusDays(FLEX_OFFER_EXPIRATION_DAYS).withTime(0, 0, 0, 0));
            if (decision == 0) {
                // create a flex offer for the flex request: put a populated flex offer.
                LOGGER.debug("A new FlexOffer will be created for FlexRequest with sequence [{}]",
                        flexRequestDto.getSequenceNumber());
                populateFlexOfferDto(flexOfferDto, flexRequestDto, true);
                outputFlexOffers.add(flexOfferDto);
            } else if (decision == 1) {
                // refuse to create an offer for the flex request: put an empty flex offer.
                LOGGER.debug("An empty FlexOffer will be created for FlexRequest with sequence [{}]",
                        flexRequestDto.getSequenceNumber());
                populateFlexOfferDto(flexOfferDto, flexRequestDto, false);
                outputFlexOffers.add(flexOfferDto);
            } else {
                // do not create a flex offer now: don't put anything new in the output list.
                LOGGER.debug("No FlexOffer will be created now for FlexRequest with sequence [{}]",
                        flexRequestDto.getSequenceNumber());
            }
        }
        context.setValue(FlexOfferDetermineFlexibilityStepParameter.OUT.FLEX_OFFER_DTO_LIST.name(), outputFlexOffers);
        LOGGER.info("AgrFlexOfferStub: complete");
        return context;
    }

    private void populateFlexOfferDto(FlexOfferDto flexOfferDto, FlexRequestDto flexRequestDto, boolean populatePtus) {
        flexOfferDto.setPeriod(flexRequestDto.getPeriod());
        flexOfferDto.setConnectionGroupEntityAddress(flexRequestDto.getConnectionGroupEntityAddress());
        flexOfferDto.setParticipantDomain(flexRequestDto.getParticipantDomain());
        if (populatePtus) {
            flexRequestDto.getPtus().stream().forEach(ptuFlexRequestDto -> {
                PtuFlexOfferDto ptuFlexOfferDto = new PtuFlexOfferDto();
                ptuFlexOfferDto.setPtuIndex(ptuFlexRequestDto.getPtuIndex());
                if (ptuFlexRequestDto.getDisposition() == DispositionTypeDto.AVAILABLE) {
                    ptuFlexOfferDto.setPrice(BigDecimal.ZERO);
                    ptuFlexOfferDto.setPower(BigInteger.ZERO);
                } else {
                    max100PercentOfOriginalPower(ptuFlexRequestDto, ptuFlexOfferDto);
                }
                flexOfferDto.getPtus().add(ptuFlexOfferDto);
            });
        }
    }

    private void max100PercentOfOriginalPower(PtuFlexRequestDto ptuFlexRequestDto, PtuFlexOfferDto ptuFlexOfferDto) {
        BigDecimal percentage = BigDecimal.valueOf(Math.abs(new Random().nextDouble() % 1));
        // Offer up to 0% of the requested power and max 100%
        ptuFlexOfferDto.setPower(
                percentage.multiply(new BigDecimal(ptuFlexRequestDto.getPower()), MathContext.DECIMAL64).toBigInteger());
        ptuFlexOfferDto.setPrice(
                BigDecimal.valueOf(0.4).multiply(new BigDecimal(ptuFlexOfferDto.getPower()), MathContext.DECIMAL64));
    }
}
