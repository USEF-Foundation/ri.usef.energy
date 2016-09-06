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

package energy.usef.brp.workflow.plan.flexorder.place;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.service.business.BrpPlanboardBusinessService;
import energy.usef.brp.workflow.BrpWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOrder;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PrognosisResponse;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;
import energy.usef.core.workflow.exception.WorkflowException;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.FlexOfferTransformer;
import energy.usef.core.workflow.transformer.FlexOrderTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class for the workflow describing the placement of Flex Orders on the BRP side.
 */
@Singleton
public class BrpFlexOrderCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrpFlexOrderCoordinator.class);

    @Inject
    private Config config;

    @Inject
    private ConfigBrp configBrp;

    @Inject
    private BrpPlanboardBusinessService brpPlanboardBusinessService;

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * This method hanldes a {@link FlexOrderEvent}.
     *
     * @param event {@link FlexOrderEvent}.
     */
    public void handleEvent(@Observes FlexOrderEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        // find processable flex offers
        List<PlanboardMessage> acceptedOffers = brpPlanboardBusinessService.findOrderableFlexOffers();

        Map<String, List<PlanboardMessage>> offersPerConnectionGroup = categorizePerConnectionGroup(acceptedOffers);

        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        // per connection group loop.
        for (Entry<String, List<PlanboardMessage>> offersPerConnectionGroupEntry : offersPerConnectionGroup.entrySet()) {

            List<FlexOfferDto> allOfferDtos = createFlexOffersDtoWithPtus(offersPerConnectionGroupEntry.getValue());
            List<FlexOfferDto> offerDtos = removeNotDesirableFlexOffers(allOfferDtos, offersPerConnectionGroupEntry.getKey());

            // group offerDtos by date
            Map<LocalDate, List<FlexOfferDto>> offerDtosPerDate = offerDtos.stream()
                    .collect(Collectors.groupingBy(FlexOfferDto::getPeriod));

            // loop per day
            for (Entry<LocalDate, List<FlexOfferDto>> dateEntry : offerDtosPerDate.entrySet()) {
                List<Long> acceptedFlexOffers = invokePBCPlaceFlexOrders(ptuDuration, offersPerConnectionGroupEntry.getKey(),
                        dateEntry.getKey(), dateEntry.getValue());

                storeAndSendFlexOrders(offersPerConnectionGroupEntry.getValue(), offerDtos, acceptedFlexOffers);
            }
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    @SuppressWarnings("unchecked")
    private List<Long> invokePBCPlaceFlexOrders(int ptuDuration, String usefIdentifier,
            LocalDate period,
            List<FlexOfferDto> flexOfferDtos) {
        WorkflowContext workflowContext = new DefaultWorkflowContext();
        workflowContext.setValue(PlaceFlexOrdersStepParameter.IN.CONNECTION_GROUP_IDENTIFIER.name(), usefIdentifier);
        workflowContext.setValue(PlaceFlexOrdersStepParameter.IN.FLEX_OFFER_DTO_LIST.name(), flexOfferDtos);
        workflowContext.setValue(PlaceFlexOrdersStepParameter.IN.PTU_DURATION.name(), ptuDuration);
        workflowContext.setValue(PlaceFlexOrdersStepParameter.IN.PERIOD.name(), period);

        // determine which flex offers are accepted. The flex offers which will be accepted are turned into flex order.
        workflowContext = workflowStepExecuter.invoke(BrpWorkflowStep.BRP_PLACE_FLEX_ORDERS.name(), workflowContext);

        // validate context
        WorkflowUtil.validateContext(
                BrpWorkflowStep.BRP_PLACE_FLEX_ORDERS.name(), workflowContext, PlaceFlexOrdersStepParameter.OUT.values());

        // store the accepted offers as flex orders (per ptu).
        return workflowContext.get(PlaceFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), List.class);
    }

    private List<FlexOfferDto> removeNotDesirableFlexOffers(List<FlexOfferDto> offerDtos, String connectionGroupIdentifier) {

        // Map: Flex Offer Sequence Number -> PlanboardMessage
        Map<Long, PlanboardMessage> flexOfferSequenceNumberToAPlanMap = new HashMap<>();
        // Map: PlanboardMessage Sequence Number -> Flex Offer Count
        Map<Long, Integer> aPlanSequenceNumberToFlexOfferCountMap = new HashMap<>();
        // Map: PlanboardMessage Sequence Number -> PlanboardMessage
        Map<Long, PlanboardMessage> aPlanMap = new HashMap<>();

        for (FlexOfferDto flexOfferDto : offerDtos) {
            PlanboardMessage aPlan = corePlanboardBusinessService
                    .findAPlanRelatedToFlexOffer(flexOfferDto.getSequenceNumber(), connectionGroupIdentifier);
            if (aPlan != null) {
                flexOfferSequenceNumberToAPlanMap.put(flexOfferDto.getSequenceNumber(), aPlan);

                int flexOfferCount = aPlanSequenceNumberToFlexOfferCountMap.getOrDefault(aPlan.getSequence(), 0);
                flexOfferCount++;
                aPlanSequenceNumberToFlexOfferCountMap.put(aPlan.getSequence(), flexOfferCount);

                if (aPlanMap.get(aPlan.getSequence()) == null) {
                    aPlanMap.put(aPlan.getSequence(), aPlan);
                }
            }
        }

        List<Long> notDesirableFlexOfferSequences = invokePBCGetNotDesirableFlexOffers(offerDtos, connectionGroupIdentifier);
        LOGGER.debug(
                "Got {} not desirable flex offers for the connection group identifier {}. No flex order will be created for the offers, corresponding A-Plans will be approved.",
                notDesirableFlexOfferSequences,
                connectionGroupIdentifier);

        // Map: PlanboardMessage Sequence Number -> Flex Offer Count
        Map<Long, Integer> aPlanSequenceNumberToNotDesirableFlexOfferCountMap = new HashMap<>();
        for (Long flexOfferSequenceNumber : notDesirableFlexOfferSequences) {
            if (!flexOfferSequenceNumberToAPlanMap.containsKey(flexOfferSequenceNumber)) {
                continue;
            }

            Long aPlanSequenceNumber = flexOfferSequenceNumberToAPlanMap.get(flexOfferSequenceNumber).getSequence();
            int count = aPlanSequenceNumberToNotDesirableFlexOfferCountMap.getOrDefault(aPlanSequenceNumber, 0);
            count++;
            aPlanSequenceNumberToNotDesirableFlexOfferCountMap.put(aPlanSequenceNumber, count);
        }

        for (Entry<Long, Integer> entry : aPlanSequenceNumberToNotDesirableFlexOfferCountMap.entrySet()) {
            if (entry.getValue().equals(aPlanSequenceNumberToFlexOfferCountMap.get(entry.getKey()))) {
                approveAPlan(aPlanMap.get(entry.getKey()));
            }
        }

        return offerDtos.stream()
                .filter(offerDto -> !notDesirableFlexOfferSequences.contains(offerDto.getSequenceNumber()))
                .map(Function.identity()).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Long> invokePBCGetNotDesirableFlexOffers(List<FlexOfferDto> offerDtos, String connectionGroupIdentifier) {
        WorkflowContext inputContext = new DefaultWorkflowContext();
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        // Creating input context
        inputContext.setValue(GetNotDesirableFlexOffersParameter.IN.PTU_DURATION.name(), ptuDuration);
        inputContext.setValue(GetNotDesirableFlexOffersParameter.IN.CONNECTION_GROUP_IDENTIFIER.name(),
                connectionGroupIdentifier);
        inputContext.setValue(GetNotDesirableFlexOffersParameter.IN.FLEX_OFFER_DTO_LIST.name(), offerDtos);
        // Invoking PBC
        WorkflowContext outputContext = workflowStepExecuter.invoke(BrpWorkflowStep.BRP_GET_NOT_DESIRABLE_FLEX_OFFERS.name(), inputContext);

        // Validating context
        WorkflowUtil.validateContext(BrpWorkflowStep.BRP_GET_NOT_DESIRABLE_FLEX_OFFERS.name(), outputContext,
                GetNotDesirableFlexOffersParameter.OUT.values());

        // Getting not desirable flex offer sequences
        return outputContext.get(GetNotDesirableFlexOffersParameter.OUT.NOT_DESIRABLE_FLEX_OFFER_SEQUENCE_LIST.name(), List.class);
    }

    private void approveAPlan(PlanboardMessage aPlanMessage) {
        if (aPlanMessage.getDocumentStatus() == DocumentStatus.ACCEPTED) {
            return;
        }

        // Updating A-Plan status to ACCEPTED
        aPlanMessage.setDocumentStatus(DocumentStatus.ACCEPTED);

        // Sending A-Plan response
        if (aPlanMessage.getMessage() != null) {
            sendAcceptedPrognosisResponse(aPlanMessage.getParticipantDomain(), aPlanMessage.getSequence(), aPlanMessage
                    .getMessage().getConversationId());
        } else {
            throw new WorkflowException("Impossible to send a response since the initial message has not been found.");
        }

        LOGGER.debug("Approved the A-Plan with the sequence number {}", aPlanMessage.getSequence());
    }

    private void sendAcceptedPrognosisResponse(String recipientDomain, Long aPlanSequence, String conversationId) {
        PrognosisResponse prognosisResponse = createPrognosisResponse(conversationId, recipientDomain, aPlanSequence);
        putMessageIntoOutgoingQueue(prognosisResponse);
    }

    private PrognosisResponse createPrognosisResponse(String conversationID, String recipientDomain, Long aPlanSequence) {
        PrognosisResponse prognosisResponse = new PrognosisResponse();

        MessageMetadata messageMetadata = new MessageMetadataBuilder().precedence(ROUTINE)
                .messageID()
                .timeStamp()
                .conversationID(conversationID)
                .senderDomain(config.getProperty(ConfigParam.HOST_DOMAIN))
                .senderRole(USEFRole.BRP)
                .recipientDomain(recipientDomain)
                .recipientRole(USEFRole.AGR)
                .build();

        prognosisResponse.setPrognosisSequence(aPlanSequence);

        LOGGER.info("A-Plan accepted");
        prognosisResponse.setResult(DispositionAcceptedRejected.ACCEPTED);

        prognosisResponse.setMessageMetadata(messageMetadata);

        return prognosisResponse;
    }

    private void putMessageIntoOutgoingQueue(PrognosisResponse xmlObject) {
        String xml = XMLUtil.messageObjectToXml(xmlObject);
        jmsHelperService.sendMessageToOutQueue(xml);
    }

    /**
     * Categorizes a list of planboard messages per USEF identifier of the connection group.
     *
     * @param acceptedOffers {@link java.util.List} of {@link PlanboardMessage} of type
     * {@link energy.usef.core.model.DocumentType#FLEX_OFFER} with the status {@link DocumentStatus#ACCEPTED}.
     * @return {@link java.util.Map} with the USEF identifier of the connection group as key ({@link String}) and a {@link List} of
     * {@link PlanboardMessage} as value.
     */
    private Map<String, List<PlanboardMessage>> categorizePerConnectionGroup(List<PlanboardMessage> acceptedOffers) {
        return acceptedOffers.stream()
                .collect(Collectors.groupingBy(offer -> offer.getConnectionGroup().getUsefIdentifier(), Collectors.toList()));
    }

    private List<FlexOfferDto> createFlexOffersDtoWithPtus(List<PlanboardMessage> offers) {
        List<FlexOfferDto> offerDtos = new ArrayList<>();
        for (PlanboardMessage offer : offers) {
            Map<Integer, PtuFlexOffer> ptuFlexOffers = corePlanboardBusinessService.findPtuFlexOffer(offer.getSequence(),
                    offer.getParticipantDomain());
            FlexOfferDto flexOfferDto = FlexOfferTransformer.transformPtuFlexOffers(new ArrayList<>(ptuFlexOffers.values()));
            if (flexOfferDto != null) {
                offerDtos.add(flexOfferDto);
            }
        }
        return offerDtos;
    }

    private void storeAndSendFlexOrders(List<PlanboardMessage> newOffers, List<FlexOfferDto> offerDtos,
            List<Long> sequenceOfAcceptedOffers) {
        for (Long sequenceOfOffer : sequenceOfAcceptedOffers) {
            PlanboardMessage offer = getOfferBySequence(newOffers, sequenceOfOffer);
            FlexOfferDto offerDto = getOfferDtoBySequence(offerDtos, sequenceOfOffer);

            // create and send flex order message.
            FlexOrder flexOrderMessage = createFlexOrderMessage(offer, offerDto, DateTimeUtil.getEndOfDay(offerDto.getPeriod()));

            // store flex order on the planboard.
            corePlanboardBusinessService.storeFlexOrder(flexOrderMessage.getMessageMetadata().getRecipientDomain(),
                    flexOrderMessage, DocumentStatus.SENT, flexOrderMessage.getMessageMetadata().getRecipientDomain(),
                    AcknowledgementStatus.SENT);

            jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(flexOrderMessage));

            // set the offer status on PROCESSED, so it won't be processed again.
            offer.setDocumentStatus(DocumentStatus.PROCESSED);
        }
    }

    private FlexOrder createFlexOrderMessage(PlanboardMessage offer, FlexOfferDto offerDto, LocalDateTime validUntil) {
        FlexOrder flexOrderMessage = new FlexOrder();

        flexOrderMessage.setMessageMetadata(MessageMetadataBuilder.build(offer.getParticipantDomain(), USEFRole.AGR,
                config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.BRP, TRANSACTIONAL).validUntil(validUntil).build());
        flexOrderMessage.setCurrency(config.getProperty(ConfigParam.CURRENCY));
        flexOrderMessage.setTimeZone(config.getProperty(ConfigParam.TIME_ZONE));
        flexOrderMessage.setPTUDuration(Period.minutes(config.getIntegerProperty(ConfigParam.PTU_DURATION)));
        flexOrderMessage.setFlexOfferOrigin(offer.getParticipantDomain());
        flexOrderMessage.setFlexOfferSequence(offer.getSequence());
        flexOrderMessage.setSequence(sequenceGeneratorService.next());
        flexOrderMessage.setPeriod(offer.getPeriod());
        flexOrderMessage.setExpirationDateTime(validUntil);
        flexOrderMessage.setOrderReference(UUID.randomUUID().toString());

        flexOrderMessage.getPTU()
                .addAll(PtuListConverter.compact(offerDto.getPtus()
                        .stream()
                        .map(PtuFlexOrderDto::new)
                        .map(FlexOrderTransformer::transformPtuFlexOrderDtoToPtu)
                        .collect(Collectors.toList())));
        return flexOrderMessage;
    }

    private PlanboardMessage getOfferBySequence(List<PlanboardMessage> newOffers, Long sequenceOfOffer) {
        for (PlanboardMessage offer : newOffers) {
            if (sequenceOfOffer.equals(offer.getSequence())) {
                return offer;
            }
        }
        throw new TechnicalException("Could not find offer with sequence " + sequenceOfOffer +
                ". The step should return a valid sequence id of the offer.");
    }

    private FlexOfferDto getOfferDtoBySequence(List<FlexOfferDto> offerDtos, Long sequenceOfOffer) {
        return offerDtos.stream().filter(offerDto -> sequenceOfOffer.equals(offerDto.getSequenceNumber())).findFirst().get();
    }

}
