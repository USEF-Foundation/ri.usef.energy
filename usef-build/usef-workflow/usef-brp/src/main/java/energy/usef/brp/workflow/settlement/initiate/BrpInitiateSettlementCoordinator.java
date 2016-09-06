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

package energy.usef.brp.workflow.settlement.initiate;

import static energy.usef.brp.workflow.BrpWorkflowStep.BRP_REQUEST_PENALTY_DATA;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;
import static energy.usef.core.workflow.settlement.CoreInitiateSettlementParameter.IN.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.brp.controller.MeterDataQueryResponseController;
import energy.usef.brp.model.MeterDataCompany;
import energy.usef.brp.service.business.BrpBusinessService;
import energy.usef.brp.workflow.BrpWorkflowStep;
import energy.usef.brp.workflow.settlement.initiate.RequestPenaltyDataParameter.IN;
import energy.usef.brp.workflow.settlement.send.CheckInitiateSettlementDoneEvent;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.constant.USEFConstants;
import energy.usef.core.data.xml.bean.message.Connections;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.MeterDataQuery;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.MeterDataQueryMessageUtil;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.coordinator.AbstractSettlementCoordinator;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.SettlementDto;
import energy.usef.core.workflow.transformer.MeterDataTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;

/**
 * BRP Initiate Settlement workflow coordinator. This workflow is cut into two parts:
 * <p>
 * <li>1. prepare initiate settlement sends a {@link MeterDataQuery}</li>
 * <li>2. finalise initiate settlement triggered by the {@link MeterDataQueryResponseController}
 * workflow.</li>
 */
@Stateless
@LocalBean
public class BrpInitiateSettlementCoordinator extends AbstractSettlementCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrpInitiateSettlementCoordinator.class);

    @Inject
    private ConfigBrp configBrp;

    @Inject
    private BrpBusinessService brpBusinessService;

    @Inject
    private Event<CheckInitiateSettlementDoneEvent> checkInitiateSettlementDoneEvent;

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;
    /**
     * Preparation of the Initiate Settlement process for the first day of the previous month until the last day of the previous
     * month. The initiating of settlements will stop if there are no flex orders in the selected period.
     *
     * @param event The {@link CollectSmartMeterDataEvent} triggering the process.
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void handleCollectSmartMeterDataEvent(
            @Observes(during = TransactionPhase.AFTER_COMPLETION) CollectSmartMeterDataEvent event) {
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT, event);
        LocalDate dayOneMonthBefore = event.getPeriodInMonth();
        if (dayOneMonthBefore == null) {
            dayOneMonthBefore = DateTimeUtil.getCurrentDate().minusMonths(1);
        }
        LocalDate startDate = dayOneMonthBefore.withDayOfMonth(1);
        LocalDate endDate = dayOneMonthBefore.dayOfMonth().withMaximumValue();

        // retrieve a distinct list of all connections valid in given time frame and map them to a string list.
        Map<LocalDate, Map<ConnectionGroup, List<Connection>>> connectionGroupsWithConnections = corePlanboardBusinessService
                .findConnectionGroupWithConnectionsWithOverlappingValidity(startDate, endDate);
        List<LocalDate> daysWithOrders = corePlanboardBusinessService
                .findPlanboardMessages(DocumentType.FLEX_ORDER, startDate, endDate, DocumentStatus.ACCEPTED).stream()
                .map(PlanboardMessage::getPeriod).distinct().collect(Collectors.toList());
        if (daysWithOrders.isEmpty()) {
            checkInitiateSettlementDoneEvent.fire(new CheckInitiateSettlementDoneEvent(startDate.getYear(), startDate.getMonthOfYear()));
            LOGGER.debug(USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
            return;
        }
        // loop through all the MDCs
        for (MeterDataCompany meterDataCompany : brpBusinessService.findAllMDCs()) {
            // query meter data company (MDC) for all connections
            LOGGER.info("Preparing sending MeterDataQuery to Meter Data Company {}", meterDataCompany.getDomain());
            for (LocalDate period : connectionGroupsWithConnections.keySet()) {
                if (!daysWithOrders.contains(period)) {
                    continue;
                }
                LocalDateTime expirationDateTime = DateTimeUtil.getCurrentDateTime()
                        .plusHours(configBrp.getIntegerProperty(ConfigBrpParam.BRP_METER_DATA_QUERY_EXPIRATION_IN_HOURS));
                MessageMetadata messageMetadata = MessageMetadataBuilder.build(meterDataCompany.getDomain(), USEFRole.MDC,
                        config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.BRP, TRANSACTIONAL)
                        .validUntil(expirationDateTime)
                        .build();

                // fill the MeterDataQuery message
                MeterDataQuery meterDataQuery = new MeterDataQuery();
                meterDataQuery.setMessageMetadata(messageMetadata);
                meterDataQuery.setDateRangeStart(period);
                meterDataQuery.setDateRangeEnd(period);
                meterDataQuery.getConnections().addAll(buildConnectionGroups(connectionGroupsWithConnections.get(period)));
                MeterDataQueryMessageUtil.populateConnectionsInConnectionGroups(meterDataQuery,
                        connectionGroupsWithConnections.get(period));

                // Store in PlanboardMessage, no connectionGroup available because query is for the whole grid.
                // the period should be the startDate of the month.
                PlanboardMessage meterDataQueryPlanboardMessage = new PlanboardMessage(DocumentType.METER_DATA_QUERY_USAGE,
                        sequenceGeneratorService.next(), DocumentStatus.SENT, meterDataCompany.getDomain(), period, null,
                        null, expirationDateTime);
                corePlanboardBusinessService.updatePlanboardMessage(meterDataQueryPlanboardMessage);

                // send the message
                jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(meterDataQuery));
            }
        }
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private List<Connections> buildConnectionGroups(Map<ConnectionGroup, List<Connection>> connectionGroupsWithConnections) {
        return connectionGroupsWithConnections.keySet()
                .stream()
                .map(ConnectionGroup::getUsefIdentifier)
                .map(usefIdentifier -> {
                    Connections mdqConnectionGroup = new Connections();
                    mdqConnectionGroup.setParent(usefIdentifier);
                    return mdqConnectionGroup;
                })
                .collect(Collectors.toList());
    }

    /**
     * Fired every few minutes ({@link ConfigBrpParam#BRP_METER_DATA_QUERY_EXPIRATION_CHECK_INTERVAL_IN_MINUTES}) to check if
     * there are unfinished initiate settlement workflows older than x hours (
     * {@link ConfigBrpParam#BRP_METER_DATA_QUERY_EXPIRATION_IN_HOURS}) that need to be finished.
     *
     * @param event The {@link FinalizeUnfinishedInitiateSettlementEvent} triggering the process.
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void finalizeUnfinishedInitiateSettlements(@Observes FinalizeUnfinishedInitiateSettlementEvent event) {
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT, event);
        LocalDateTime expirationDateTime = DateTimeUtil.getCurrentDateTime()
                .minusHours(configBrp.getIntegerProperty(ConfigBrpParam.BRP_METER_DATA_QUERY_EXPIRATION_IN_HOURS));

        List<PlanboardMessage> meterDataQueryUsageMessages = corePlanboardBusinessService.findPlanboardMessagesOlderThan(
                expirationDateTime, DocumentType.METER_DATA_QUERY_USAGE, DocumentStatus.SENT);
        meterDataQueryUsageMessages.forEach(meterDataQueryUsageMessage -> {
            meterDataQueryUsageMessage.setDocumentStatus(DocumentStatus.EXPIRED);
            LOGGER.error("No MeterDataQueryResponse received from Meter Data Company {} after {} hours. Unable to start "
                            + "settlement process!", meterDataQueryUsageMessage.getParticipantDomain(),
                    ConfigBrpParam.BRP_METER_DATA_QUERY_EXPIRATION_IN_HOURS);
        });
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Handles the event triggering the initiation of the settlement.
     *
     * @param finalizeInitiateSettlementEvent {@link FinalizeInitiateSettlementEvent}.
     */
    @SuppressWarnings("unchecked")
    @Asynchronous
    public void handleBrpInitiateSettlement(
            @Observes(during = TransactionPhase.AFTER_COMPLETION) FinalizeInitiateSettlementEvent finalizeInitiateSettlementEvent) {
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT, finalizeInitiateSettlementEvent);
        // variables
        final LocalDate startDate = finalizeInitiateSettlementEvent.getStartDate();
        final LocalDate endDate = finalizeInitiateSettlementEvent.getEndDate();
        // creation of the context and call to the PBC.
        WorkflowContext inContext = initiateWorkflowContext(startDate, endDate);
        inContext.setValue(BrpInitiateSettlementParameter.IN.SMART_METER_DATA.name(),
                MeterDataTransformer.transform(finalizeInitiateSettlementEvent.getConnectionGroupList()));
        SettlementDto settlementDto = invokeInitiateSettlementPbc(inContext);

        // Add the settlement prices to the SettlementDto
        settlementDto = calculateSettlementPrice(settlementDto, inContext.get(FLEX_OFFER_DTO_LIST.name(), List.class));

        // invoke the PBC to add penalty data to the ptuSettlementList
        settlementDto = addPenaltyData(settlementDto);
        // save the settlement dtos.
        saveSettlement(settlementDto);
        LOGGER.info("Settlements are stored in the DB.");
        checkInitiateSettlementDoneEvent.fire(new CheckInitiateSettlementDoneEvent(startDate.getYear(), startDate.getMonthOfYear()));
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT, finalizeInitiateSettlementEvent);
    }

    /**
     * Gets the workflow name which will be used to invoke the PBC.
     *
     * @return {@link String} the workflow name.
     */
    @Override
    public String getWorkflowName() {
        return BrpWorkflowStep.BRP_INITIATE_SETTLEMENT.name();
    }

    /**
     * Initializes the workflow context with all the relevant information which will be given to the PBC.
     *
     * @param startDate {@link LocalDate} start date of the settlement period (inclusive).
     * @param endDate {@link LocalDate} end date of the settlement period (inclusive).
     * @return a {@link WorkflowContext} object.
     */
    @Override
    public WorkflowContext initiateWorkflowContext(LocalDate startDate, LocalDate endDate) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(START_DATE.name(), startDate);
        inContext.setValue(END_DATE.name(), endDate);
        inContext.setValue(PROGNOSIS_DTO_LIST.name(), fetchRelevantPrognoses(startDate, endDate));
        inContext.setValue(FLEX_REQUEST_DTO_LIST.name(), fetchRelevantFlexRequests(startDate, endDate));
        List<FlexOfferDto> flexOfferDtos = fetchRelevantFlexOffers(startDate, endDate);
        inContext.setValue(FLEX_OFFER_DTO_LIST.name(), flexOfferDtos);
        inContext.setValue(FLEX_ORDER_DTO_LIST.name(), fetchRelevantFlexOrders(startDate, endDate, flexOfferDtos));
        inContext.setValue(IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        return inContext;
    }

    /**
     * Invokes request penalty data PBC to add penalty data to list of {@link SettlementDto} entities.
     *
     * @param settlementDto the {@link SettlementDto} containing the settlement information.
     * @return List with modified {@link SettlementDto} entities
     */
    @SuppressWarnings("unchecked")
    private SettlementDto addPenaltyData(SettlementDto settlementDto) {
        // Invoking PBC
        WorkflowContext inputContext = new DefaultWorkflowContext();
        inputContext.setValue(IN.SETTLEMENT_DTO.name(), settlementDto);
        inputContext.setValue(IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));

        WorkflowContext outputContext = workflowStepExecuter.invoke(BRP_REQUEST_PENALTY_DATA.name(), inputContext);

        // Validating context
        WorkflowUtil.validateContext(BRP_REQUEST_PENALTY_DATA.name(), outputContext, RequestPenaltyDataParameter.OUT.values());
        return outputContext.get(RequestPenaltyDataParameter.OUT.UPDATED_SETTLEMENT_DTO.name(), SettlementDto.class);
    }

}
