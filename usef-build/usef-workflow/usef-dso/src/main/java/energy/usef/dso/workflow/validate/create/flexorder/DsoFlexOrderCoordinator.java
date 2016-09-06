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

package energy.usef.dso.workflow.validate.create.flexorder;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexOrder;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.PlanboardMessage;
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
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.FlexOfferTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This coordinater executes the business logic for Creating and Sending FlexOffer's.
 */
@Singleton
public class DsoFlexOrderCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoFlexOrderCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStubLoader;

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private Config config;

    @Inject
    private ConfigDso configDso;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * This method will create flex order and send them based on the planboard.
     *
     * @param event The {@link FlexOrderEvent} event.
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void handleEvent(@Observes FlexOrderEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        // Map: Congestion point entity address -> Map: PTU Date -> Flex Offer list
        Map<String, Map<LocalDate, List<PlanboardMessage>>> offersPerCongestionPointPerDateMap = dsoPlanboardBusinessService
                .findOrderableFlexOffers();

        // Loop per congestion point
        for (Entry<String, Map<LocalDate, List<PlanboardMessage>>> congestionPointEntry : offersPerCongestionPointPerDateMap
                .entrySet()) {
            // Loop per PTU date
            for (Entry<LocalDate, List<PlanboardMessage>> dateEntry : congestionPointEntry.getValue().entrySet()) {
                List<FlexOfferDto> flexOfferDtos = createFlexOffersDtoWithPtus(dateEntry.getValue());

                // invoke the PBC
                List<Long> acceptedFlexOffers = invokePlaceFlexOrdersPBC(flexOfferDtos, congestionPointEntry.getKey(),
                        dateEntry.getKey());

                if (!acceptedFlexOffers.isEmpty()) {
                    LOGGER.debug(
                            "Flex offers for Entity Address: {}, PTU Date: {} are accepted. Required flex orders will be placed ",
                            congestionPointEntry.getKey(), dateEntry.getKey());
                    // Storing the accepted flex offers and placing required flex orders
                    storeAndSendFlexOrders(offersPerCongestionPointPerDateMap, acceptedFlexOffers, flexOfferDtos,
                            congestionPointEntry.getKey(), dateEntry.getKey());
                } else {
                    LOGGER.warn("Flex offers for Entity Address: {}, PTU Date: {} are not accepted. No flex order will be placed ",
                            congestionPointEntry.getKey(), dateEntry.getKey());
                }
            }

        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    @SuppressWarnings("unchecked") private List<Long> invokePlaceFlexOrdersPBC(List<FlexOfferDto> flexOfferDtos, String congestionPoint, LocalDate period) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(PlaceFlexOrdersStepParameter.IN.FLEX_OFFER_DTO_LIST.name(), flexOfferDtos);
        inContext.setValue(PlaceFlexOrdersStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), congestionPoint);
        inContext.setValue(PlaceFlexOrdersStepParameter.IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        inContext.setValue(PlaceFlexOrdersStepParameter.IN.PERIOD.name(), period);

        // determine which flex offers are accepted. The flex offers which will be accepted are turned into flex order.
        WorkflowContext outContext = workflowStubLoader.invoke(DsoWorkflowStep.DSO_PLACE_FLEX_ORDERS.name(), inContext);

        // validate context
        WorkflowUtil.validateContext(DsoWorkflowStep.DSO_PLACE_FLEX_ORDERS.name(), outContext, PlaceFlexOrdersStepParameter.OUT.values());

        return outContext.get(PlaceFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), List.class);
    }

    private void storeAndSendFlexOrders(Map<String, Map<LocalDate, List<PlanboardMessage>>> flexOffersPerCongestionPointPerDateMap,
            List<Long> acceptedFlexOffers, List<FlexOfferDto> flexOfferDtos, String congestionPoint, LocalDate period) {
        for (Long flexOfferSequence : acceptedFlexOffers) {

            PlanboardMessage acceptedFlexOffer = getOfferByCongestionPointDateAndSequence(flexOffersPerCongestionPointPerDateMap,
                    congestionPoint, period, flexOfferSequence);
            Long flexOrderSequence = sequenceGeneratorService.next();

            FlexOfferDto currentFlexOfferDto = flexOfferDtos.stream()
                    .filter(flexOfferDto -> flexOfferDto.getSequenceNumber().equals(flexOfferSequence)).findFirst().orElse(null);

            if (currentFlexOfferDto == null) {
                LOGGER.error("Unable to fetch flex offer by sequence number {} while this flex offer is expected!");
                continue;
            }

            // create and send flex order message.
            FlexOrder flexOrderMessage = createFlexOrderMessage(acceptedFlexOffer, currentFlexOfferDto, flexOrderSequence,
                    DateTimeUtil.getEndOfDay(period));

            // store flex order on the planboard.
            corePlanboardBusinessService.storeFlexOrder(flexOrderMessage.getCongestionPoint(), flexOrderMessage,
                    DocumentStatus.SENT, flexOrderMessage.getMessageMetadata().getRecipientDomain(), AcknowledgementStatus.SENT);

            jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(flexOrderMessage));

            // set the offer status on PROCESSED, so it won't be processed again.
            acceptedFlexOffer.setDocumentStatus(DocumentStatus.PROCESSED);
        }
    }

    private FlexOrder createFlexOrderMessage(PlanboardMessage offer, FlexOfferDto offerDto, Long flexOrderSequence,
            LocalDateTime validUntil) {
        FlexOrder flexOrderMessage = new FlexOrder();

        flexOrderMessage.setMessageMetadata(MessageMetadataBuilder.build(offer.getParticipantDomain(), USEFRole.AGR,
                config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.DSO, ROUTINE).validUntil(validUntil).build());
        flexOrderMessage.setCongestionPoint(offer.getConnectionGroup().getUsefIdentifier());
        flexOrderMessage.setCurrency(config.getProperty(ConfigParam.CURRENCY));
        flexOrderMessage.setTimeZone(config.getProperty(ConfigParam.TIME_ZONE));
        flexOrderMessage.setPTUDuration(Period.minutes(config.getIntegerProperty(ConfigParam.PTU_DURATION)));
        flexOrderMessage.setFlexOfferOrigin(offer.getParticipantDomain());
        flexOrderMessage.setFlexOfferSequence(offer.getSequence());
        flexOrderMessage.setSequence(flexOrderSequence);
        flexOrderMessage.setPeriod(offer.getPeriod());
        flexOrderMessage.setExpirationDateTime(validUntil);
        flexOrderMessage.setOrderReference(UUID.randomUUID().toString());

        offerDto.getPtus().stream().map(FlexOfferTransformer::transformToPTU).collect(Collectors.toList());

        List<PTU> ptus = offerDto.getPtus().stream()
                .map(FlexOfferTransformer::transformToPTU)
                .collect(Collectors.toList());
        flexOrderMessage.getPTU().addAll(PtuListConverter.compact(ptus));
        return flexOrderMessage;
    }

    private PlanboardMessage getOfferByCongestionPointDateAndSequence(
            Map<String, Map<LocalDate, List<PlanboardMessage>>> flexOffersPerCongestionPointPerDate, String congestionPoint,
            LocalDate period, Long flexOfferSequenceNumber) {

        List<PlanboardMessage> flexOffers = flexOffersPerCongestionPointPerDate.get(congestionPoint).get(period);
        return flexOffers.stream().filter(o -> o.getSequence().equals(flexOfferSequenceNumber)).findAny().get();
    }

    private List<FlexOfferDto> createFlexOffersDtoWithPtus(List<PlanboardMessage> offers) {
        List<FlexOfferDto> offerDtos = offers.stream().map(FlexOfferTransformer::transform).collect(Collectors.toList());

        for (FlexOfferDto offerDto : offerDtos) {
            offerDto.setPtus(
                    corePlanboardBusinessService.findPtuFlexOffer(offerDto.getSequenceNumber(), offerDto.getParticipantDomain())
                            .values().stream().map(FlexOfferTransformer::transformPtuFlexOffer).collect(Collectors.toList()));
        }
        return offerDtos;
    }

}
