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

package energy.usef.core.workflow.coordinator;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Exchange;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;
import energy.usef.core.workflow.dto.PtuSettlementDto;
import energy.usef.core.workflow.dto.SettlementDto;
import energy.usef.core.workflow.settlement.CoreInitiateSettlementParameter;
import energy.usef.core.workflow.settlement.CoreSettlementBusinessService;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.FlexOfferTransformer;
import energy.usef.core.workflow.transformer.FlexOrderTransformer;
import energy.usef.core.workflow.transformer.FlexRequestTransformer;
import energy.usef.core.workflow.transformer.PrognosisTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.LocalDate;

/**
 * Abstract implementation of a SettlementCoordinator implementing shared methods.
 */
public abstract class AbstractSettlementCoordinator {

    private static final Function<Exchange, String> getDocumentKey = exchange -> "" + exchange.getConnectionGroup()
            .getUsefIdentifier() + exchange.getSequence() + exchange.getParticipantDomain();
    private static final int PRICE_PRECISION = 4;

    @Inject
    protected Config config;

    @Inject
    protected CoreSettlementBusinessService coreSettlementBusinessService;

    @Inject
    protected CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    protected WorkflowStepExecuter workflowStepExecuter;

    protected abstract String getWorkflowName();

    protected abstract WorkflowContext initiateWorkflowContext(LocalDate startDate, LocalDate endDate);

    protected void validateWorkflowContext(WorkflowContext outContext) {
        WorkflowUtil.validateContext(getWorkflowName(), outContext, CoreInitiateSettlementParameter.OUT.values());
    }

    /**
     * Fetches the Prognoses DTO relevant for the settlement process.
     */
    public List<PrognosisDto> fetchRelevantPrognoses(LocalDate startDate, LocalDate endDate) {
        return new ArrayList<>(coreSettlementBusinessService.findRelevantPrognoses(startDate, endDate)
                .stream().collect(groupingBy(getDocumentKey::apply,
                        Collectors.collectingAndThen(toList(), PrognosisTransformer::mapToPrognosis))).values());
    }

    /**
     * Fetches the Flex Requests DTO relevant for the settlement process.
     */
    public List<FlexRequestDto> fetchRelevantFlexRequests(LocalDate startDate, LocalDate endDate) {
        return new ArrayList<>(coreSettlementBusinessService.findRelevantFlexRequests(startDate, endDate)
                .stream().collect(groupingBy(getDocumentKey::apply,
                        Collectors.collectingAndThen(toList(), FlexRequestTransformer::transformFlexRequest))).values());
    }

    /**
     * Fetches the Flex Offers DTO relevant for the settlement process.
     */
    public List<FlexOfferDto> fetchRelevantFlexOffers(LocalDate startDate, LocalDate endDate) {
        return new ArrayList<>(coreSettlementBusinessService.findRelevantFlexOffers(startDate, endDate)
                .stream().collect(groupingBy(getDocumentKey::apply,
                        Collectors.collectingAndThen(toList(), FlexOfferTransformer::transformPtuFlexOffers))).values());
    }

    /**
     * Fetches the Flex Orders DTO relevant for the settlement process.
     */
    public List<FlexOrderDto> fetchRelevantFlexOrders(LocalDate startDate, LocalDate
            endDate, List<FlexOfferDto> flexOfferDtos) {
        Map<String, FlexOfferDto> flexOffersMap = flexOfferDtos.stream()
                .collect(toMap(flexOfferDto -> "" + flexOfferDto.getSequenceNumber() + flexOfferDto.getParticipantDomain(),
                        Function.identity()));
        List<FlexOrderDto> flexOrderDtos = new ArrayList<>(
                coreSettlementBusinessService.findRelevantFlexOrders(startDate, endDate)
                        .stream().collect(groupingBy(getDocumentKey::apply,
                        Collectors.collectingAndThen(toList(), FlexOrderTransformer::transformPtuFlexOrders))).values());
        flexOrderDtos.stream().forEach(flexOrderDto -> {
            FlexOfferDto flexOfferDto = flexOffersMap
                    .get("" + flexOrderDto.getFlexOfferSequenceNumber() + flexOrderDto.getParticipantDomain());
            for (PtuFlexOfferDto ptuFlexOfferDto : flexOfferDto.getPtus()) {
                int index = ptuFlexOfferDto.getPtuIndex().intValue() - 1;
                flexOrderDto.getPtus().get(index).setPower(ptuFlexOfferDto.getPower());
                flexOrderDto.getPtus().get(index).setPrice(ptuFlexOfferDto.getPrice());
            }
        });

        List<PlanboardMessage> acceptedOffers = corePlanboardBusinessService
                .findPlanboardMessages(DocumentType.FLEX_ORDER, startDate, endDate, DocumentStatus.ACCEPTED);
        List<PlanboardMessage> processedOffers = corePlanboardBusinessService
                .findPlanboardMessages(DocumentType.FLEX_ORDER, startDate, endDate, DocumentStatus.PROCESSED);
        processedOffers.addAll(acceptedOffers);
        Map<String, PlanboardMessage> flexOrders = processedOffers.stream()
                .collect(Collectors.toMap(pm -> "" + pm.getParticipantDomain() + pm.getSequence(), Function.identity()));

        for (FlexOrderDto flexOrderDto : flexOrderDtos) {
            PlanboardMessage flexOrder = flexOrders.get(flexOrderDto.getParticipantDomain() + flexOrderDto.getSequenceNumber());

            if (flexOrder.getCreationDateTime().toLocalDate().isEqual(flexOrderDto.getPeriod())) {
                int currentPtuIndex = PtuUtil
                        .getPtuIndex(flexOrder.getCreationDateTime(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
                for (int ptuIndex = 0; ptuIndex < currentPtuIndex; ptuIndex++) {
                    flexOrderDto.getPtus().get(ptuIndex).setPower(BigInteger.ZERO);
                    flexOrderDto.getPtus().get(ptuIndex).setPrice(BigDecimal.ZERO);
                }
            }
        }
        return flexOrderDtos;
    }

    /**
     * Saves the SettlementDto received from the PBC.
     *
     * @param settlementDto {@link SettlementDto} object.
     */

    public void saveSettlement(SettlementDto settlementDto) {
        coreSettlementBusinessService.createFlexOrderSettlements(settlementDto.getFlexOrderSettlementDtos());
    }

    /**
     * Invokes the Pluggable Business Component in charge of the initialization of the settlement. Outcoming workflow context
     * will be validated and settlement items will be save in the database.
     *
     * @param inContext {@link WorkflowContext} input context.
     */
    public SettlementDto invokeInitiateSettlementPbc(WorkflowContext inContext) {
        WorkflowContext outContext = workflowStepExecuter.invoke(getWorkflowName(), inContext);
        validateWorkflowContext(outContext);
        return outContext.get(CoreInitiateSettlementParameter.OUT.SETTLEMENT_DTO.name(), SettlementDto.class);
    }

    /**
     * Computes pro-rate settlement prices for all flex orders where not all ordered power is delivered based on the
     * calculation:
     * <p>
     * PTU-Settlement.Price = DeliveredFlexPower * (FlexOrder.Price / OrderedFlexPower)
     *
     * @param settlementDto The {@link SettlementDto} containing the settlement info needed to do the calculations.
     * @param flexOfferDtos List of {@link FlexOfferDto} objects containing the ordered flex.
     * @return {@link SettlementDto} with updated prices
     */
    public SettlementDto calculateSettlementPrice(SettlementDto settlementDto, List<FlexOfferDto> flexOfferDtos) {
        Map<String, FlexOfferDto> flexOffersByParticipantDomainPlusSequence = flexOfferDtos.stream()
                .collect(Collectors.toMap(flexOffer -> flexOffer.getParticipantDomain() + flexOffer.getSequenceNumber(),
                        flexOffer -> flexOffer));

        for (FlexOrderSettlementDto flexOrderSettlementDto : settlementDto.getFlexOrderSettlementDtos()) {
            FlexOfferDto flexOffer = flexOffersByParticipantDomainPlusSequence
                    .get(flexOrderSettlementDto.getFlexOrder().getParticipantDomain() + flexOrderSettlementDto.getFlexOrder()
                            .getFlexOfferSequenceNumber());
            Map<Integer, PtuFlexOfferDto> ptuFlexOfferPerPtu = flexOffer.getPtus().stream()
                    .collect(Collectors.toMap(ptuFlexOfferDto -> ptuFlexOfferDto.getPtuIndex().intValue(),
                            ptuFlexOfferDto -> ptuFlexOfferDto));

            for (PtuSettlementDto ptuSettlementDto : flexOrderSettlementDto.getPtuSettlementDtos()) {
                // only calculate the price if there is ordered flex power
                if (ptuSettlementDto.getOrderedFlexPower().compareTo(BigInteger.ZERO) != 0) {
                    BigDecimal ptuFlexOrderPrice = ptuFlexOfferPerPtu.get(ptuSettlementDto.getPtuIndex()
                            .intValue()).getPrice();
                    BigDecimal price = new BigDecimal(ptuSettlementDto.getDeliveredFlexPower())
                            .multiply(ptuFlexOrderPrice.divide(new BigDecimal(ptuSettlementDto.getOrderedFlexPower()),
                                    PRICE_PRECISION,
                                    BigDecimal.ROUND_UP));
                    ptuSettlementDto.setPrice(price);
                } else {
                    ptuSettlementDto.setPrice(BigDecimal.ZERO);
                }
            }
        }

        return settlementDto;
    }
}
