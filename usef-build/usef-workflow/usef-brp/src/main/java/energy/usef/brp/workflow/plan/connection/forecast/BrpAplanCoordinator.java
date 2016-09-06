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

package energy.usef.brp.workflow.plan.connection.forecast;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.brp.service.business.BrpPlanboardBusinessService;
import energy.usef.brp.workflow.plan.flexrequest.create.CreateFlexRequestEvent;
import energy.usef.brp.workflow.BrpWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PrognosisResponse;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.exception.WorkflowException;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.PrognosisTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BRP A-Plan workflow coordinator.
 */
@Singleton
public class BrpAplanCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrpAplanCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private BrpPlanboardBusinessService brpPlanboardBusinessService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private Config config;

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private Event<PrepareFlexRequestsEvent> prepareFlexRequestsEventManager;

    @Inject
    private Event<CreateFlexRequestEvent> createFlexRequestEventManager;

    @Inject
    private EventValidationService eventValidationService;


    /**
     * Invokes the PBC in charge of deciding whether A-Plans for the given period are accepted.
     *
     * @param event {@link ReceivedAPlanEvent}
     */
    public void receivedAPlanEvent(@Observes ReceivedAPlanEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);

        // Making incoming context
        List<PrognosisDto> aplanDtos = fetchAllAPlans(event.getPeriod());

        // Retrieve the received A-Plans
        List<PrognosisDto> receivedAplans = fetchAllLatestAPlans(event.getPeriod(), DocumentStatus.RECEIVED);

        WorkflowContext outContext = invokePBCReceivedAplan(aplanDtos, receivedAplans);

        // Getting accepted A-Plan DTOs
        @SuppressWarnings("unchecked")
        List<PrognosisDto> acceptedAPlans = (List<PrognosisDto>) outContext.getValue(
                ReceivedAPlanWorkflowParameter.OUT.ACCEPTED_A_PLAN_DTO_LIST.name());
        // Getting processed A-Plan DTOs
        @SuppressWarnings("unchecked")
        List<PrognosisDto> processedAPlans = (List<PrognosisDto>) outContext.getValue(
                ReceivedAPlanWorkflowParameter.OUT.PROCESSED_A_PLAN_DTO_LIST.name());

        for (PrognosisDto aPlan : receivedAplans) {
            PlanboardMessage aPlanMessage = corePlanboardBusinessService
                    .findSinglePlanboardMessage(aPlan.getSequenceNumber(), DocumentType.A_PLAN, aPlan.getParticipantDomain());
            if (contains(acceptedAPlans, aPlan)) {
                // Updating A-Plan status to ACCEPTED
                aPlanMessage.setDocumentStatus(DocumentStatus.ACCEPTED);

                // Sending prognosis response
                if (aPlanMessage.getMessage() != null) {
                    sendAcceptedPrognosisResponse(aPlan, aPlanMessage.getMessage().getConversationId());
                } else {
                    throw new WorkflowException("Impossible to send a response since the initial message has not been found.");
                }
            } else if (contains(processedAPlans, aPlan)) {
                // Updating A-Plan status to PROCESSED
                aPlanMessage.setDocumentStatus(DocumentStatus.PROCESSED);
                // No prognosis response should be sent at this moment
            }
        }

        // Triggering the PrepareFlexRequestsEvent
        prepareFlexRequestsEventManager.fire(new PrepareFlexRequestsEvent(event.getPeriod()));
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private WorkflowContext invokePBCReceivedAplan(List<PrognosisDto> aplanDtos, List<PrognosisDto> receivedAplans) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(ReceivedAPlanWorkflowParameter.IN.A_PLAN_DTO_LIST.name(), aplanDtos);
        inContext.setValue(ReceivedAPlanWorkflowParameter.IN.RECEIVED_A_PLAN_DTO_LIST.name(), receivedAplans);
        inContext.setValue(ReceivedAPlanWorkflowParameter.IN.PTU_DURATION.name(),
                config.getIntegerProperty(ConfigParam.PTU_DURATION));

        // Invoking the PBC
        WorkflowContext outContext = workflowStepExecuter.invoke(BrpWorkflowStep.BRP_RECEIVED_APLAN.name(), inContext);

        // Validating outgoing context
        WorkflowUtil.validateContext(BrpWorkflowStep.BRP_RECEIVED_APLAN.name(), outContext, ReceivedAPlanWorkflowParameter.OUT.values());
        return outContext;
    }

    /**
     * Invokes the PBC in charge of deciding whether flex requests for the given period are to be sent.
     *
     * @param event {@link PrepareFlexRequestsEvent}
     */
    @SuppressWarnings("unchecked")
    public void prepareFlexRequestsEvent(@Observes PrepareFlexRequestsEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);

        // Making incoming context
        List<PrognosisDto> processedAPlanDtos = fetchAllLatestAPlans(event.getPeriod(), DocumentStatus.PROCESSED);

        WorkflowContext outContext = invokePBCPrepareFlexRequests(processedAPlanDtos);

        // Sending flex requests
        List<FlexRequestDto> flexRequests = (List<FlexRequestDto>) outContext.getValue(
                PrepareFlexRequestWorkflowParameter.OUT.FLEX_REQUEST_DTO_LIST.name());
        LOGGER.debug("Sending flex requests. [{}] flex requests will be generated and sent by another coordinator.",
                flexRequests.size());
        if (!flexRequests.isEmpty()) {
            createFlexRequestEventManager.fire(new CreateFlexRequestEvent(flexRequests));
        }

        flexRequests.stream().forEach(flexRequest -> {
            PlanboardMessage aPlanMessage = corePlanboardBusinessService
                    .findSinglePlanboardMessage(flexRequest.getPrognosisSequenceNumber(), DocumentType.A_PLAN,
                            flexRequest.getParticipantDomain());
            // Updating A-Plan status to PENDING_FLEX_TRADING
            aPlanMessage.setDocumentStatus(DocumentStatus.PENDING_FLEX_TRADING);
        });

        // Sending accepted prognosis responses
        LOGGER.debug("Sending accepted prognosis responses.");
        List<PrognosisDto> acceptedAPlans = (List<PrognosisDto>) outContext.getValue(
                PrepareFlexRequestWorkflowParameter.OUT.ACCEPTED_A_PLAN_DTO_LIST.name());
        processedAPlanDtos
                .stream()
                .filter(aPlan -> contains(acceptedAPlans, aPlan))
                .forEach(aplanDto -> {
                    PlanboardMessage aPlanMessage = corePlanboardBusinessService.findSinglePlanboardMessage(
                            aplanDto.getSequenceNumber(), DocumentType.A_PLAN, aplanDto.getParticipantDomain());
                    // Updating A-Plan status to ACCEPTED
                        aPlanMessage.setDocumentStatus(DocumentStatus.ACCEPTED);
                        // Sending prognosis response
                        if (aPlanMessage.getMessage() != null) {
                            sendAcceptedPrognosisResponse(aplanDto, aPlanMessage.getMessage().getConversationId());
                        } else {
                            throw new WorkflowException(
                                    "Impossible to send a response since the initial message has not been found.");
                        }
                    });
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    public WorkflowContext invokePBCPrepareFlexRequests(List<PrognosisDto> processedAPlanDtos) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(PrepareFlexRequestWorkflowParameter.IN.PROCESSED_A_PLAN_DTO_LIST.name(), processedAPlanDtos);
        inContext.setValue(PrepareFlexRequestWorkflowParameter.IN.PTU_DURATION.name(),
                config.getIntegerProperty(ConfigParam.PTU_DURATION));

        // Invoking the PBC
        WorkflowContext outContext = workflowStepExecuter.invoke(BrpWorkflowStep.BRP_PREPARE_FLEX_REQUESTS.name(), inContext);

        // Validating outgoing context
        WorkflowUtil.validateContext(BrpWorkflowStep.BRP_PREPARE_FLEX_REQUESTS.name(), outContext, PrepareFlexRequestWorkflowParameter.OUT.values());
        return outContext;
    }

    private boolean contains(List<PrognosisDto> aPlans, PrognosisDto aPlan) {
        for (PrognosisDto dto : aPlans) {
            if (dto.getSequenceNumber().equals(aPlan.getSequenceNumber())
                    && dto.getParticipantDomain().equals(aPlan.getParticipantDomain())) {
                return true;
            }
        }
        return false;
    }

    private List<PrognosisDto> groupAndTransform(List<PtuPrognosis> ptuAPlans) {
        return ptuAPlans.stream()
                .collect(Collectors.groupingBy(
                        ptuAPlan -> "" + ptuAPlan.getSequence() + ptuAPlan.getParticipantDomain() + ptuAPlan.getConnectionGroup()
                                .getUsefIdentifier())).values().stream().map(PrognosisTransformer::mapToPrognosis)
                .collect(Collectors.toList());
    }

    private List<PrognosisDto> fetchAllLatestAPlans(LocalDate period, DocumentStatus documentStatus) {
        return groupAndTransform(corePlanboardBusinessService.findLastPrognoses(period, PrognosisType.A_PLAN, documentStatus));
    }

    private List<PrognosisDto> fetchAllAPlans(LocalDate period) {
        return groupAndTransform(corePlanboardBusinessService.findLastPrognoses(period, PrognosisType.A_PLAN));
    }

    private void sendAcceptedPrognosisResponse(PrognosisDto prognosis, String conversationId) {
        PrognosisResponse prognosisResponse = createPrognosisResponse(conversationId, prognosis.getParticipantDomain(),
                prognosis.getSequenceNumber());
        putMessageIntoOutgoingQueue(prognosisResponse);
    }

    private PrognosisResponse createPrognosisResponse(String conversationID, String recipientDomain, Long prognosisSequence) {
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

        prognosisResponse.setPrognosisSequence(prognosisSequence);

        LOGGER.info("A-Plan accepted");
        prognosisResponse.setResult(DispositionAcceptedRejected.ACCEPTED);

        prognosisResponse.setMessageMetadata(messageMetadata);

        return prognosisResponse;
    }

    private void putMessageIntoOutgoingQueue(PrognosisResponse xmlObject) {
        String xml = XMLUtil.messageObjectToXml(xmlObject);
        jmsHelperService.sendMessageToOutQueue(xml);
    }
}
