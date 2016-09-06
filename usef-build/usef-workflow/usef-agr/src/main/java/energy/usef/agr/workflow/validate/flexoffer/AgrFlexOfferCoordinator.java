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

package energy.usef.agr.workflow.validate.flexoffer;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexOffer;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuFlexRequest;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.FlexOfferTransformer;
import energy.usef.core.workflow.transformer.FlexRequestTransformer;
import energy.usef.core.workflow.transformer.PrognosisTransformer;
import energy.usef.core.workflow.transformer.USEFRoleTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This coordinater executes the business logic for Creating and Sending FlexOffer's.
 */
@Singleton
public class AgrFlexOfferCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrFlexOfferCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;
    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Inject
    private AgrPlanboardBusinessService agrPlanboardBusinessService;
    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Inject
    private CorePlanboardValidatorService planboardValidatorService;
    @Inject
    private JMSHelperService jmsHelperService;
    @Inject
    private Config config;
    @Inject
    private ConfigAgr configAgr;
    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * Handles the business logic triggered by a {@link FlexOfferEvent}.
     *
     * @param event {@link FlexOfferEvent} event instance which will trigger the workflow.
     */
    public void handleEvent(@Observes FlexOfferEvent event) throws BusinessException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        // get processable FlexRequests
        Map<USEFRole, List<PlanboardMessage>> flexRequestsPerRole = agrPlanboardBusinessService.findAcceptedRequests(
                DocumentType.FLEX_REQUEST)
                .stream()
                .filter(this::validateRequest)
                .collect(groupingBy(this::determineUsefRoleOfPlanboardMessage));

        for (USEFRole usefRole : flexRequestsPerRole.keySet()) {
            Map<LocalDate, List<PlanboardMessage>> flexRequestsPerPeriod = flexRequestsPerRole.get(usefRole)
                    .stream()
                    .collect(groupingBy(PlanboardMessage::getPeriod));
            for (LocalDate period : flexRequestsPerPeriod.keySet()) {
                LOGGER.info("Processing flex offers for the Role: [{}].", usefRole);

                // invoke the PBC to generate flex offers.
                List<FlexOfferDto> flexOfferDtos = generateFlexOffersInWithPBC(usefRole, period, flexRequestsPerPeriod.get(period));

                for (FlexOfferDto flexOfferDto : flexOfferDtos) {
                    PlanboardMessage associatedFlexRequest = flexRequestsPerPeriod.get(period)
                            .stream()
                            .filter(flexRequest -> flexOfferDto.getFlexRequestSequenceNumber().equals(flexRequest.getSequence()))
                            .findFirst()
                            .orElseThrow(() -> new BusinessException(
                                    () -> "The flex offer returned by the PBC has no related flex request with sequence ["
                                            + flexOfferDto.getFlexRequestSequenceNumber() + "]"));

                    // build flexoffer Message
                    FlexOffer flexOfferMessage = buildFlexOfferMessage(usefRole, flexOfferDto);
                    // store message
                    corePlanboardBusinessService.storeFlexOffer(flexOfferDto.getConnectionGroupEntityAddress(), flexOfferMessage,
                            DocumentStatus.SENT, flexOfferDto.getParticipantDomain());
                    // send message
                    jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(flexOfferMessage));
                    // mark the flex request as processed
                    associatedFlexRequest.setDocumentStatus(DocumentStatus.PROCESSED);
                }
            }
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private boolean validateRequest(PlanboardMessage flexRequestMessage) {
        String usefIdentifier = flexRequestMessage.getConnectionGroup().getUsefIdentifier();

        PtuFlexRequest flexRequest = agrPlanboardBusinessService.findLastFlexRequestDocumentWithDispositionRequested(
                usefIdentifier, flexRequestMessage.getPeriod(), flexRequestMessage.getSequence());

        if (flexRequest == null) {
            LOGGER.error(
                    "Impossible to find the latest flex request with disposition requested for: connectionGroup={}; period={}; "
                            + "sequence={}", usefIdentifier, flexRequestMessage.getPeriod(), flexRequestMessage.getSequence());
            throw new TechnicalException("Impossible to validate flex requests. Impossible situation.");
        }
        if (planboardValidatorService.isPlanboardItemWithingIntradayGateClosureTime(flexRequest)) {
            flexRequestMessage.setDocumentStatus(DocumentStatus.EXPIRED);
            LOGGER.warn("No FlexOFfer Sent, IntradayGateClosure has passed for Requested flexibility. ");
            return false;
        }
        if (flexRequestMessage.getExpirationDate().isBefore(DateTimeUtil.getCurrentDateTime())) {
            flexRequestMessage.setDocumentStatus(DocumentStatus.EXPIRED);
            LOGGER.info("FlexRequest {} ignored, it expired {}.", flexRequestMessage.getSequence(),
                    flexRequestMessage.getExpirationDate());
            return false;
        }
        return true;
    }

    private Map<String, List<String>> buildConnectionGroupToConnectionsMap(List<PlanboardMessage> flexRequestsPerPeriod,
            LocalDate period) {
        List<String> connectionGroupUsefIdentifier = flexRequestsPerPeriod.stream()
                .map(flexRequestMessage -> flexRequestMessage.getConnectionGroup().getUsefIdentifier())
                .distinct()
                .collect(toList());
        Map<ConnectionGroup, List<Connection>> connectionsWithConnectionGroups = corePlanboardBusinessService
                .findConnectionsWithConnectionGroups(connectionGroupUsefIdentifier, period);
        return connectionsWithConnectionGroups.entrySet().stream()
                .collect(toMap(
                        entry -> entry.getKey().getUsefIdentifier(),
                        entry -> entry.getValue().stream().map(Connection::getEntityAddress).collect(toList())));
    }

    @SuppressWarnings("unchecked")
    private List<FlexOfferDto> generateFlexOffersInWithPBC(USEFRole usefRole, LocalDate period,
            List<PlanboardMessage> flexRequestsForPeriod) {

        List<PrognosisDto> aPlanDtos = corePlanboardBusinessService.findLastPrognoses(period, PrognosisType.A_PLAN)
                .stream()
                .collect(groupingBy(ptuAPlan -> "" + ptuAPlan.getParticipantDomain() + ptuAPlan.getSequence() + ptuAPlan
                        .getConnectionGroup().getUsefIdentifier())).values().stream().map(PrognosisTransformer::mapToPrognosis)
                .collect(toList());

        List<PrognosisDto> dPrognosisDtos = agrPlanboardBusinessService.findLastPrognoses(period,
                PrognosisType.D_PROGNOSIS,
                Optional.empty());

        List<FlexRequestDto> flexRequestDtos = getFlexRequestDtos(usefRole, flexRequestsForPeriod);

        // fetch connections related to the connection groups for the given period
        Map<String, List<String>> connectionGroupToConnectionAddressMap = buildConnectionGroupToConnectionsMap(
                flexRequestsForPeriod, period);

        WorkflowContext workflowContext = new DefaultWorkflowContext();
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.FLEX_REQUEST_DTO_LIST.name(), flexRequestDtos);
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.PERIOD.name(), period);
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.LATEST_A_PLANS_DTO_LIST.name(), aPlanDtos);
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.LATEST_D_PROGNOSES_DTO_LIST.name(), dPrognosisDtos);
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(), connectionGroupToConnectionAddressMap);
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.CONNECTION_PORTFOLIO_DTO.name(),
                agrPortfolioBusinessService.findConnectionPortfolioDto(period));
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.FLEX_OFFER_DTO_LIST.name(), fetchPlacedFlexOffersForPeriod(period));

        // determine flexibility
        workflowContext = workflowStepExecuter.invoke(AgrWorkflowStep.AGR_FLEX_OFFER_DETERMINE_FLEXIBILITY.name(), workflowContext);

        // validate context
        WorkflowUtil.validateContext(AgrWorkflowStep.AGR_FLEX_OFFER_DETERMINE_FLEXIBILITY.name(), workflowContext, FlexOfferDetermineFlexibilityStepParameter.OUT
                .values());

        return (List<FlexOfferDto>) workflowContext.getValue(FlexOfferDetermineFlexibilityStepParameter.OUT.FLEX_OFFER_DTO_LIST.name());
    }

    private List<FlexRequestDto> getFlexRequestDtos(USEFRole usefRole, List<PlanboardMessage> flexRequestsForPeriod) {
        List<FlexRequestDto> flexRequestDtoList = new ArrayList<>();

        for (PlanboardMessage flexRequest : flexRequestsForPeriod) {
            List<PtuFlexRequest> ptuFlexRequests = agrPlanboardBusinessService.findPtuFlexRequestWithSequence(
                    flexRequest.getConnectionGroup().getUsefIdentifier(), flexRequest.getSequence(),
                    flexRequest.getParticipantDomain());

            if (!ptuFlexRequests.isEmpty()) {
                FlexRequestDto dto = FlexRequestTransformer.transformFlexRequest(ptuFlexRequests);
                dto.setParticipantRole(USEFRoleTransformer.transform(usefRole));
                dto.setExpirationDateTime(flexRequest.getExpirationDate());
                flexRequestDtoList.add(dto);
            }
        }

        return flexRequestDtoList;
    }

    private List<FlexOfferDto> fetchPlacedFlexOffersForPeriod(LocalDate period) {
        return new ArrayList<>(corePlanboardBusinessService.findPlacedFlexOffers(period).stream()
                .collect(groupingBy(ptuFlexOffer -> "" + ptuFlexOffer.getParticipantDomain() + ptuFlexOffer.getSequence(),
                        collectingAndThen(toList(), FlexOfferTransformer::transformPtuFlexOffers))).values());
    }

    private FlexOffer buildFlexOfferMessage(USEFRole recipientRole, FlexOfferDto flexOfferDto) {
        FlexOffer flexOffer = new FlexOffer();

        flexOffer.setMessageMetadata(MessageMetadataBuilder.build(flexOfferDto.getParticipantDomain(), recipientRole,
                config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.AGR, ROUTINE).validUntil(flexOfferDto.getExpirationDateTime())
                .build());
        if (USEFRole.DSO == recipientRole) {
            flexOffer.setCongestionPoint(flexOfferDto.getConnectionGroupEntityAddress());
        }

        flexOffer.setExpirationDateTime(flexOfferDto.getExpirationDateTime());
        flexOffer.setFlexRequestOrigin(flexOfferDto.getParticipantDomain());
        flexOffer.setFlexRequestSequence(flexOfferDto.getFlexRequestSequenceNumber());
        flexOffer.setSequence(sequenceGeneratorService.next());
        flexOffer.setPeriod(flexOfferDto.getPeriod());
        flexOffer.setTimeZone(config.getProperty(ConfigParam.TIME_ZONE));
        flexOffer.setCurrency(config.getProperty(ConfigParam.CURRENCY));
        flexOffer.setPTUDuration(Period.minutes(config.getIntegerProperty(ConfigParam.PTU_DURATION)));
        if (!flexOfferDto.getPtus().isEmpty()) {
            flexOffer.getPTU()
                    .addAll(PtuListConverter.compact(flexOfferDto.getPtus()
                            .stream()
                            .map(FlexOfferTransformer::transformToPTU)
                            .collect(toList())));
        }
        return flexOffer;
    }

    private USEFRole determineUsefRoleOfPlanboardMessage(PlanboardMessage message) {
        if (message.getConnectionGroup() instanceof BrpConnectionGroup) {
            return USEFRole.BRP;
        } else if (message.getConnectionGroup() instanceof CongestionPointConnectionGroup) {
            return USEFRole.DSO;
        } else {
            return null;
        }
    }
}
