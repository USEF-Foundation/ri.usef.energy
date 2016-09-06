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

package energy.usef.dso.workflow.settlement.initiate;

import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;
import static energy.usef.core.workflow.settlement.CoreInitiateSettlementParameter.IN.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.constant.USEFConstants;
import energy.usef.core.data.xml.bean.message.Connections;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.MeterDataQuery;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.CongestionPointConnectionGroup;
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
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;
import energy.usef.dso.controller.MeterDataQueryResponseController;
import energy.usef.dso.model.AggregatorOnConnectionGroupState;
import energy.usef.dso.model.MeterDataCompany;
import energy.usef.dso.model.PtuGridMonitor;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.dto.GridMonitoringDto;
import energy.usef.dso.workflow.settlement.send.CheckInitiateSettlementDoneEvent;
import energy.usef.dso.workflow.transformer.GridMonitoringTransformer;

/**
 * DSO Initiate Settlement workflow coordinator. This workflow is cut into two parts:
 * <p>
 * <li>1. prepare initiate settlement sends a {@link MeterDataQuery} <li>2. finalise initiate
 * settlement triggered by the {@link MeterDataQueryResponseController} workflow.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
@LocalBean
public class DsoInitiateSettlementCoordinator extends AbstractSettlementCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoInitiateSettlementCoordinator.class);

    @Inject
    private Config config;

    @Inject
    private ConfigDso configDso;

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Inject
    private Event<FinalizeInitiateSettlementEvent> finalizeInitiateSettlementEventManager;

    @Inject
    private Event<CheckInitiateSettlementDoneEvent> checkInitiateSettlementDoneEvent;

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * Preparation of the Initiate Settlement process.
     *
     * @param event the {@link CollectSmartMeterDataEvent} that triggers the process.
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

        LOGGER.info("Preparing Initiate Settlement workflow for the start day {} and end day {}.", startDate, endDate);

        // retrieve a distinct list of all connections valid in given time frame and map them to a string list.
        Map<LocalDate, Map<ConnectionGroup, List<Connection>>> connectionGroupsWithConnections = corePlanboardBusinessService
                .findConnectionGroupWithConnectionsWithOverlappingValidity(startDate, endDate);
        List<LocalDate> daysWithOrders = corePlanboardBusinessService
                .findPlanboardMessages(DocumentType.FLEX_ORDER, startDate, endDate, DocumentStatus.ACCEPTED).stream()
                .map(PlanboardMessage::getPeriod).distinct().collect(toList());
        if (daysWithOrders.isEmpty()) {
            checkInitiateSettlementDoneEvent
                    .fire(new CheckInitiateSettlementDoneEvent(startDate.getYear(), startDate.getMonthOfYear()));
            LOGGER.debug(USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
            return;
        }
        // loop through all the MDCs
        for (MeterDataCompany meterDataCompany : dsoPlanboardBusinessService.findAllMDCs()) {
            // query meter data company (MDC) for all connections
            LOGGER.info("Preparing sending MeterDataQuery to Meter Data Company {}", meterDataCompany.getDomain());
            for (LocalDate period : connectionGroupsWithConnections.keySet()) {
                if (!daysWithOrders.contains(period)) {
                    continue;
                }
                LocalDateTime expirationDateTime = DateTimeUtil.getCurrentDateTime()
                        .plusHours(configDso.getIntegerProperty(ConfigDsoParam.DSO_METER_DATA_QUERY_EXPIRATION_IN_HOURS));
                MessageMetadata messageMetadata = MessageMetadataBuilder.build(meterDataCompany.getDomain(), USEFRole.MDC,
                        config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.DSO, TRANSACTIONAL).validUntil(expirationDateTime)
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
                        null, null);
                meterDataQueryPlanboardMessage.setExpirationDate(expirationDateTime);
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
                .collect(toList());
    }

    /**
     * Fired every few minutes ({@link ConfigDsoParam#DSO_METER_DATA_QUERY_EXPIRATION_CHECK_INTERVAL_IN_MINUTES}) to check if there
     * are unfinished initiate settlement workflows older than x hours ( {@link
     * ConfigDsoParam#DSO_METER_DATA_QUERY_EXPIRATION_IN_HOURS})
     * that need to be finished.
     *
     * @param event The {@link FinalizeUnfinishedInitiateSettlementEvent} that triggers the process.
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void finalizeUnfinishedInitiateSettlements(@Observes FinalizeUnfinishedInitiateSettlementEvent event) {
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT, event);
        LocalDateTime expirationDateTime = DateTimeUtil.getCurrentDateTime()
                .minusHours(configDso.getIntegerProperty(ConfigDsoParam.DSO_METER_DATA_QUERY_EXPIRATION_IN_HOURS));

        List<PlanboardMessage> meterDataQueryUsageMessages = corePlanboardBusinessService.findPlanboardMessagesOlderThan(
                expirationDateTime, DocumentType.METER_DATA_QUERY_USAGE, DocumentStatus.SENT);
        meterDataQueryUsageMessages.forEach(meterDataQueryUsageMessage -> {
            meterDataQueryUsageMessage.setDocumentStatus(DocumentStatus.PROCESSED);
            finalizeInitiateSettlementEventManager
                    .fire(new FinalizeInitiateSettlementEvent(meterDataQueryUsageMessage.getPeriod(),
                            meterDataQueryUsageMessage.getPeriod(), null));
        });
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Handles the event triggering the initiation of the settlement.
     *
     * @param finalizeInitiateSettlementEvent {@link CollectSmartMeterDataEvent}.
     */
    @SuppressWarnings("unchecked")
    @Asynchronous
    public void handleDsoInitiateSettlement(
            @Observes(during = TransactionPhase.AFTER_COMPLETION) FinalizeInitiateSettlementEvent finalizeInitiateSettlementEvent) {
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT, finalizeInitiateSettlementEvent);
        final LocalDate startDate = finalizeInitiateSettlementEvent.getStartDate();
        final LocalDate endDate = finalizeInitiateSettlementEvent.getEndDate();
        // creation of the context and call to the PBC.
        WorkflowContext inContext = initiateWorkflowContext(startDate, endDate);
        if (!finalizeInitiateSettlementEvent.getMeterDataPerCongestionPoint().isEmpty()) {
            inContext.setValue(DsoInitiateSettlementParameter.IN.SMART_METER_DATA.name(), MeterDataTransformer.transform(
                    finalizeInitiateSettlementEvent.getMeterDataPerCongestionPoint()));
        } else {
            inContext.setValue(DsoInitiateSettlementParameter.IN.GRID_MONITORING_DATA.name(),
                    buildGridMonitoringDtos(startDate, endDate));
        }
        SettlementDto settlementDto = invokeInitiateSettlementPbc(inContext);

        // Add the settlement prices to the SettlementDto
        settlementDto = calculateSettlementPrice(settlementDto, inContext.get(FLEX_OFFER_DTO_LIST.name(), List.class));

        // invoke the PBC to add penalty data to the ptuSettlementList
        settlementDto = addPenaltyData(settlementDto);

        // save the settlement dtos.
        saveSettlement(settlementDto);
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
        return DsoWorkflowStep.DSO_INITIATE_SETTLEMENT.name();
    }

    /**
     * Initializes the workflow context with all the relevant information which will be given to the PBC.
     *
     * @param startDate {@link LocalDate} start date of the settlement period (inclusive).
     * @param endDate   {@link LocalDate} end date of the settlement period (inclusive).
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
        inContext.setValue(RequestPenaltyDataParameter.IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        return inContext;
    }

    /**
     * Invokes request penalty data PBC to add penalty data to list of {@link SettlementDto} entities.
     *
     * @param settlementDto The {@link SettlementDto} containing the settlement info.
     * @return List with modified {@link SettlementDto} entities
     */
    private SettlementDto addPenaltyData(SettlementDto settlementDto) {
        // Invoking PBC
        WorkflowContext inputContext = new DefaultWorkflowContext();
        inputContext.setValue(RequestPenaltyDataParameter.IN.SETTLEMENT_DTO.name(), settlementDto);
        inputContext
                .setValue(RequestPenaltyDataParameter.IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));

        WorkflowContext outputContext = workflowStepExecuter.invoke(DsoWorkflowStep.DSO_REQUEST_PENALTY_DATA.name(), inputContext);

        // Validating context
        WorkflowUtil.validateContext(DsoWorkflowStep.DSO_REQUEST_PENALTY_DATA.name(), outputContext,
                RequestPenaltyDataParameter.OUT.values());
        return outputContext.get(RequestPenaltyDataParameter.OUT.UPDATED_SETTLEMENT_DTO.name(), SettlementDto.class);
    }

    private List<GridMonitoringDto> buildGridMonitoringDtos(LocalDate startDate, LocalDate endDate) {
        Map<CongestionPointConnectionGroup, List<AggregatorOnConnectionGroupState>> connectionGroupsWithAggregators =
                fetchAggregatorsPerConnectionGroup(
                        startDate, endDate);
        Map<ConnectionGroup, Map<LocalDate, List<PtuGridMonitor>>> gridMonitoringMap = dsoPlanboardBusinessService
                .findGridMonitoringData(startDate, endDate).stream()
                .collect(groupingBy(PtuGridMonitor::getConnectionGroup,
                        groupingBy(ptuGridMonitor -> ptuGridMonitor.getPtuContainer().getPtuDate(), toList())));
        List<GridMonitoringDto> gridMonitoringDtos = new ArrayList<>();
        for (ConnectionGroup congestionPoint : gridMonitoringMap.keySet()) {
            for (LocalDate period : gridMonitoringMap.get(congestionPoint).keySet()) {
                GridMonitoringDto gridMonitoringDto = GridMonitoringTransformer
                        .transform(gridMonitoringMap.get(congestionPoint).get(period));
                fetchConnectionCountPerAggregator(congestionPoint, period, connectionGroupsWithAggregators)
                        .forEach((agr, count) -> gridMonitoringDto.getConnectionCountPerAggregator().put(agr, count));
                gridMonitoringDtos.add(gridMonitoringDto);
            }
        }
        return gridMonitoringDtos;
    }

    private Map<CongestionPointConnectionGroup, List<AggregatorOnConnectionGroupState>> fetchAggregatorsPerConnectionGroup(
            LocalDate startDate, LocalDate endDate) {
        return dsoPlanboardBusinessService.findAggregatorsWithOverlappingActivityForPeriod(startDate, endDate)
                .stream()
                .collect(Collectors.groupingBy(AggregatorOnConnectionGroupState::getCongestionPointConnectionGroup));
    }

    private Map<String, Integer> fetchConnectionCountPerAggregator(ConnectionGroup congestionPoint, LocalDate period,
            Map<CongestionPointConnectionGroup, List<AggregatorOnConnectionGroupState>> connectionGroupsWithAggregators) {
        return connectionGroupsWithAggregators.getOrDefault(congestionPoint, new ArrayList<>())
                .stream()
                .filter(state -> !state.getValidFrom().isAfter(period) && state.getValidUntil().isAfter(period))
                .collect(toMap(state -> state.getAggregator().getDomain(), state -> state.getConnectionCount().intValue()));

    }

}
