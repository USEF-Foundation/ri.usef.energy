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

import energy.usef.brp.pbcfeederimpl.PbcFeederService;
import energy.usef.brp.workflow.plan.connection.forecast.PrepareFlexRequestWorkflowParameter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PowerUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.*;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the PBC in charge of handling received A-Plans and asking the generation of flex requests. This step will
 * receive the list of A-Plan data per ptu for a given period (day).
 * This stub accepts all A-Plans, so no flex requests are created
 *
 * This implementation expects to find the following parameters as input:
 * <ul>
 * <li>PTU_DURATION: PTU duration ({@link Integer})</li>
 * <li>PROCESSED_A_PLAN_DTO_LIST: A-Plan DTO list ({@link java.util.List}) of {@link PrognosisDto}</li>
 * </ul>
 * This implementation must return the following parameters as input:
 * <ul>
 * <li>FLEX_REQUEST_DTO_LIST: List of flex requests ({@link java.util.List}) of {@link FlexRequestDto}</li>
 * <li>ACCEPTED_A_PLAN_DTO_LIST: List of accepted A-Plans ({@link java.util.List}) of {@link PrognosisDto}</li>
 * </ul>
 * The list of processed A-Plan DTOs is iterated and for each item a random decision is taken: accepted, flex request is created, no
 * decision.
 */
public class BrpPrepareFlexRequestsStubAcceptAPlans implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrpPrepareFlexRequestsStubAcceptAPlans.class);

    private static final BigDecimal PERCENTAGE_DEVIATION = new BigDecimal("0.20");
    private static final BigDecimal PERCENTAGE_REDUCTION = new BigDecimal("0.50");
    private static final int FLEX_REQUEST_EXPIRATION_DAYS = 4;

    @Inject
    private PbcFeederService pbcFeederService;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("BrpPrepareFlexRequests Stub invoked");

        List<FlexRequestDto> flexRequestDtos = new ArrayList<>();
        List<PrognosisDto> acceptedAPlans = new ArrayList<>();

        List<PrognosisDto> aPlanDtos = (List<PrognosisDto>) context
                .getValue(PrepareFlexRequestWorkflowParameter.IN.PROCESSED_A_PLAN_DTO_LIST.name());
        Integer ptuDuration = (Integer) context.getValue(PrepareFlexRequestWorkflowParameter.IN.PTU_DURATION.name());
        LOGGER.debug("Input: [{}] A-Plans (with PROCESSED status).", aPlanDtos.size());

        if (!aPlanDtos.isEmpty()) {
            LocalDate aPlanDate = aPlanDtos.get(0).getPeriod();

            int numberOfPtusPerDay = PtuUtil.getNumberOfPtusPerDay(aPlanDate, ptuDuration);
            Map<Integer, BigDecimal> map = pbcFeederService.retrieveApxPrices(aPlanDate, 1, numberOfPtusPerDay);

            BigDecimal max = map.values().stream().max(BigDecimal::compareTo).get();
            BigDecimal min = map.values().stream().min(BigDecimal::compareTo).get();

            // For each received a-plan.
            for (PrognosisDto aPlanDto : aPlanDtos) {
                LOGGER.debug(
                        "A-Plan with sequence [{}] has been set to be processed for aggregator [{}]. Flex request might be generated.",
                        aPlanDto.getSequenceNumber(), aPlanDto.getParticipantDomain());
                // If rejected, create a flex request to send.
                FlexRequestDto flexRequestDto = buildMinimalFlexRequest(aPlanDto);

                boolean anyFlexRequested = false;
                for (PtuPrognosisDto ptuAPlanDto : aPlanDto.getPtus()) {
                    BigDecimal value = map.get(ptuAPlanDto.getPtuIndex().intValue());
                    // highest
                    // max.subtract(value).divide(max) <= 0.20%
                    if (max.subtract(value).divide(max, 2, RoundingMode.HALF_UP).abs().compareTo(PERCENTAGE_DEVIATION) <= 0) {
                        anyFlexRequested = true;
                        // expensive so request reduction
                        flexRequestDto.getPtus().add(buildFlexRequestRequestedPtu(ptuAPlanDto, true));
                        // lowest
                        // value.subtract(min).divide(min) <= 0.20%
                    } else if (value.subtract(min).divide(min, 2, RoundingMode.HALF_UP).abs().compareTo(PERCENTAGE_DEVIATION)
                            <= 0) {
                        anyFlexRequested = true;
                        // cheap so request increase
                        flexRequestDto.getPtus().add(buildFlexRequestRequestedPtu(ptuAPlanDto, false));
                    } else {
                        // average
                        flexRequestDto.getPtus().add(buildFlexRequestAvailablePtu(ptuAPlanDto));
                    }
                }
                if (LOGGER.isTraceEnabled()) {
                    flexRequestDto.getPtus().sort((o1, o2) -> o1.getPtuIndex().compareTo(o2.getPtuIndex()));
                    flexRequestDto.getPtus().forEach(ptu ->
                            LOGGER.trace(" PTU [{}/{}] flex request: [{}] of [{}] Wh", ptu.getPtuIndex(),
                                    numberOfPtusPerDay,
                                    ptu.getDisposition(), PowerUtil.powerToEnergy(ptu.getPower(), ptuDuration)));
                }
                acceptedAPlans.add(aPlanDto);
            }
        }

        context.setValue(PrepareFlexRequestWorkflowParameter.OUT.FLEX_REQUEST_DTO_LIST.name(), flexRequestDtos);
        context.setValue(PrepareFlexRequestWorkflowParameter.OUT.ACCEPTED_A_PLAN_DTO_LIST.name(), acceptedAPlans);

        LOGGER.debug("Output: Accepted [{}] A-Plans (status will be changed to ACCEPTED)", acceptedAPlans.size());
        LOGGER.debug("Output: Flex Requested for [{}] A-Plans (status will be changed to PENDING_FLEX_TRADING)",
                flexRequestDtos.size());

        return context;
    }

    /**
     * Creates a minimal flex request from a A-Plan.
     *
     * @param aplanDto {@link PrognosisDto} A-Plan.
     * @return a Flex Request with the period and the origin and sequence of the related prognosis.
     */
    private FlexRequestDto buildMinimalFlexRequest(PrognosisDto aplanDto) {
        FlexRequestDto flexRequestDto = new FlexRequestDto();
        flexRequestDto.setPeriod(aplanDto.getPeriod());
        flexRequestDto.setParticipantDomain(aplanDto.getParticipantDomain());
        flexRequestDto.setPrognosisSequenceNumber(aplanDto.getSequenceNumber());
        flexRequestDto.setExpirationDateTime(DateTimeUtil.getCurrentDateTime().plusDays(FLEX_REQUEST_EXPIRATION_DAYS));
        return flexRequestDto;
    }

    /**
     * Builds a ptu of a flex request from the ptu of the related A-Plan. No power is available is ever available.
     *
     * @param aplanPtu {@link PtuPrognosisDto} one of the ptus of the related A-Plan.
     * @return a PTU for the flex request.
     */
    private PtuFlexRequestDto buildFlexRequestAvailablePtu(PtuPrognosisDto aplanPtu) {
        PtuFlexRequestDto ptuFlexRequestDto = new PtuFlexRequestDto();
        ptuFlexRequestDto.setPtuIndex(aplanPtu.getPtuIndex());
        ptuFlexRequestDto.setDisposition(DispositionTypeDto.AVAILABLE);
        ptuFlexRequestDto.setPower(BigInteger.ZERO);

        return ptuFlexRequestDto;
    }

    /**
     * Builds a ptu of a flex request from the ptu of the related A-Plan where either reduction or increase of power is needed.
     *
     * @param aplanPtu  {@link PtuPrognosisDto} one of the ptus of the related A-Plan.
     * @param reduction {@link Boolean} Is there reduction or increase of power.
     * @return a PTU for the flex request.
     */
    private PtuFlexRequestDto buildFlexRequestRequestedPtu(PtuPrognosisDto aplanPtu, boolean reduction) {
        PtuFlexRequestDto ptuFlexRequestDto = new PtuFlexRequestDto();
        ptuFlexRequestDto.setPtuIndex(aplanPtu.getPtuIndex());
        ptuFlexRequestDto.setDisposition(DispositionTypeDto.REQUESTED);
        BigInteger difference = new BigDecimal(aplanPtu.getPower()).multiply(PERCENTAGE_REDUCTION)
                .setScale(0, RoundingMode.HALF_UP).toBigInteger();
        if (reduction) {
            // if this is a reduction negate the difference
            difference = difference.negate();
        }
        ptuFlexRequestDto.setPower(difference);
        return ptuFlexRequestDto;
    }
}
