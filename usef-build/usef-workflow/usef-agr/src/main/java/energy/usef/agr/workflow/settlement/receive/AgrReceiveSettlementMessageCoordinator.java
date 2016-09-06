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

package energy.usef.agr.workflow.settlement.receive;

import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.constant.USEFConstants;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOrderSettlement;
import energy.usef.core.data.xml.bean.message.FlexOrderSettlementStatus;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.SettlementMessage;
import energy.usef.core.data.xml.bean.message.SettlementMessageResponse;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.DocumentStatusUtil;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.DispositionAcceptedDisputedDto;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.USEFRoleDto;
import energy.usef.core.workflow.settlement.CoreSettlementBusinessService;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.DispositionAcceptedDisputedTransformer;
import energy.usef.core.workflow.transformer.SettlementTransformer;
import energy.usef.core.workflow.transformer.USEFRoleTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class in charge of the coordination of the business logic for the reception of {@link SettlementMessage}.
 */
@Stateless
public class AgrReceiveSettlementMessageCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrReceiveSettlementMessageCoordinator.class);
    private static final int DAYS_BEFORE_EXPIRATION = 4;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;
    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Inject
    private CoreSettlementBusinessService coreSettlementBusinessService;
    @Inject
    private Event<CheckSettlementEvent> checkSettlementEventManager;
    @Inject
    private JMSHelperService jmsHelperService;
    @Inject
    private Config config;

    /**
     * Handle Receive Settlement Message Event.
     *
     * @param event
     */
    @Asynchronous
    public void handleReceiveSettlementMessageEvent(
            @Observes(during = TransactionPhase.AFTER_COMPLETION) ReceiveSettlementMessageEvent event) {
        corePlanboardBusinessService.storeIncomingFlexOrderSettlementsPlanboardMessage(event.getMessage(), DAYS_BEFORE_EXPIRATION,
                DocumentStatus.RECEIVED, event.getMessage().getMessageMetadata().getSenderDomain(), event.getSavedMessage());
        checkSettlementEventManager.fire(new CheckSettlementEvent());
    }

    /**
     * Handle CheckSettlement Event.
     *
     * @param event
     */
    @Asynchronous
    public void handleCheckSettlementEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) CheckSettlementEvent event) {
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT, event);

        LocalDate eventDate = event.getPeriodInMonth();
        if (eventDate == null) {
            eventDate = DateTimeUtil.getCurrentDate().minusMonths(1);
        }
        LocalDate startDate = eventDate.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<PlanboardMessage> flexOrderSettlementMessages = corePlanboardBusinessService
                .findPlanboardMessages(DocumentType.FLEX_ORDER_SETTLEMENT, startDate, endDate, DocumentStatus.RECEIVED);
        LOGGER.debug("Fetched [{}] FLEX_ORDER_SETTLEMENTS from PlanboardMessage with status RECEIVED between [{}] and [{}]",
                flexOrderSettlementMessages.size(), startDate, endDate);
        Map<String, List<PlanboardMessage>> flexOrderSettlementsPerParticipant = flexOrderSettlementMessages
                .stream()
                .collect(Collectors.groupingBy(PlanboardMessage::getParticipantDomain, Collectors.toList()));
        flexOrderSettlementsPerParticipant.forEach(
                (participant, flexOrderSettlements) -> checkSettlementPerParticipant(participant, flexOrderSettlements, startDate,
                        endDate));

        LOGGER.debug(USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void checkSettlementPerParticipant(String participant, List<PlanboardMessage> flexOrderSettlements,
            LocalDate startDate, LocalDate endDate) {
        Map<Long, PlanboardMessage> flexOrderSettlementPerSequence = flexOrderSettlements
                .stream()
                .collect(Collectors.toMap(PlanboardMessage::getOriginSequence, Function.identity()));
        List<FlexOrderSettlementStatus> statuses = new ArrayList<>();

        SettlementMessage settlementMessage = (SettlementMessage) XMLUtil.xmlToMessage(
                flexOrderSettlements.get(0).getMessage().getXml());
        LOGGER.debug("Fetched SettlementMessage with ID [{}] from the database for participant [{}]",
                flexOrderSettlements.get(0).getId(), participant);
        Map<String, List<FlexOrderSettlementDto>> flexOrderSettlementDtosPerConnectionGroup = fetchPreparedFlexOrderSettlements(
                participant, startDate, endDate);

        // loop over each received flex order settlement and call the pbc to validate it against the prepared flex order
        // settlements (given per connection group).
        for (FlexOrderSettlement flexOrderSettlementXml : settlementMessage.getFlexOrderSettlement()) {
            FlexOrderSettlementDto receivedFlexOrderSettlement = SettlementTransformer.mapXmlToDto(flexOrderSettlementXml,
                    participant);
            String usefIdentifier = receivedFlexOrderSettlement.getFlexOrder().getConnectionGroupEntityAddress();
            Long orderSequence = receivedFlexOrderSettlement.getFlexOrder().getSequenceNumber();
            WorkflowContext inContext = buildWorkflowContext(
                    receivedFlexOrderSettlement,
                    flexOrderSettlementDtosPerConnectionGroup.getOrDefault(usefIdentifier, new ArrayList<>()),
                    settlementMessage.getMessageMetadata());
            WorkflowContext outContext = workflowStepExecuter
                    .invoke(AgrWorkflowStep.AGR_VALIDATE_SETTLEMENT_ITEMS.name(), inContext);
            statuses.add(handleContextOutcome(outContext, receivedFlexOrderSettlement,
                    flexOrderSettlementPerSequence.get(orderSequence)));
        }
        SettlementMessageResponse settlementMessageResponse = buildSettlementMessageResponse(settlementMessage, statuses);
        jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(settlementMessageResponse));

    }

    private Map<String, List<FlexOrderSettlementDto>> fetchPreparedFlexOrderSettlements(String participantDomain,
            LocalDate dateFrom, LocalDate dateUntil) {
        return coreSettlementBusinessService
                .findFlexOrderSettlementsForPeriod(dateFrom, dateUntil, Optional.empty(), Optional.of(participantDomain))
                .stream()
                .map(SettlementTransformer::mapModelToDto)
                .collect(Collectors.groupingBy(
                        flexOrderSettlementDto -> flexOrderSettlementDto.getFlexOrder().getConnectionGroupEntityAddress(),
                        Collectors.toList()));
    }

    private WorkflowContext buildWorkflowContext(FlexOrderSettlementDto receivedFlexOrderSettlementDto,
            List<FlexOrderSettlementDto> preparedFlexOrderSettlementDtos, MessageMetadata messageMetaData) {
        USEFRoleDto counterPartyRole = USEFRoleTransformer.transform(messageMetaData.getSenderRole());
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(AgrReceiveSettlementMessageWorkflowParameter.IN.PTU_DURATION.name(), ptuDuration);
        context.setValue(AgrReceiveSettlementMessageWorkflowParameter.IN.ORDER_REFERENCE.name(), receivedFlexOrderSettlementDto.getFlexOrder().getSequenceNumber());
        context.setValue(AgrReceiveSettlementMessageWorkflowParameter.IN.COUNTER_PARTY_ROLE.name(), counterPartyRole);
        context.setValue(AgrReceiveSettlementMessageWorkflowParameter.IN.PREPARED_FLEX_ORDER_SETTLEMENTS.name(), preparedFlexOrderSettlementDtos);
        context.setValue(AgrReceiveSettlementMessageWorkflowParameter.IN.RECEIVED_FLEX_ORDER_SETTLEMENT.name(), receivedFlexOrderSettlementDto);
        return context;
    }

    private FlexOrderSettlementStatus handleContextOutcome(WorkflowContext context, FlexOrderSettlementDto flexOrderSettlementDto,
            PlanboardMessage flexOrderSettlementMessage) {
        DispositionAcceptedDisputedDto disposition = (DispositionAcceptedDisputedDto) context.getValue(
                AgrReceiveSettlementMessageWorkflowParameter.OUT.FLEX_ORDER_SETTLEMENT_DISPOSITION.name());

        FlexOrderSettlementStatus status = new FlexOrderSettlementStatus();
        status.setDisposition(DispositionAcceptedDisputedTransformer.transformToXml(disposition));
        status.setOrderReference(String.valueOf(flexOrderSettlementDto.getFlexOrder().getSequenceNumber()));

        if (flexOrderSettlementMessage != null) {
            flexOrderSettlementMessage.setDocumentStatus(DocumentStatusUtil.toDocumentStatus(status.getDisposition()));
        }

        return status;
    }

    /**
     * Builds a {@link SettlementMessageResponse} message.
     *
     * @param settlementMessage {@link SettlementMessage} received {@link SettlementMessage}.
     * @param flexOrderSettlementStatuses List of {@link FlexOrderSettlementStatus} to send back.
     * @return a {@link SettlementMessageResponse}.
     */
    private SettlementMessageResponse buildSettlementMessageResponse(SettlementMessage settlementMessage,
            List<FlexOrderSettlementStatus> flexOrderSettlementStatuses) {
        SettlementMessageResponse response = new SettlementMessageResponse();
        response.setMessageMetadata(new MessageMetadataBuilder().messageID()
                .conversationID(settlementMessage.getMessageMetadata().getConversationID())
                .timeStamp()
                .precedence(ROUTINE)
                .senderDomain(config.getProperty(ConfigParam.HOST_DOMAIN))
                .senderRole(USEFRole.AGR)
                .recipientDomain(settlementMessage.getMessageMetadata().getSenderDomain())
                .recipientRole(settlementMessage.getMessageMetadata().getSenderRole())
                .build());
        response.setReference(settlementMessage.getReference());
        response.getFlexOrderSettlementStatus().addAll(flexOrderSettlementStatuses);
        response.setResult(DispositionAcceptedRejected.ACCEPTED);
        return response;
    }
}
