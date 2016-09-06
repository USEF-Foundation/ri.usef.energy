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

package energy.usef.dso.workflow.settlement.collect;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.ConnectionMeterEvent;
import energy.usef.core.data.xml.bean.message.Connections;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.MeterDataQuery;
import energy.usef.core.data.xml.bean.message.MeterDataQueryType;
import energy.usef.core.data.xml.bean.message.MeterDataSet;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.RegimeType;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;
import energy.usef.dso.model.MeterDataCompany;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.settlement.determine.DetermineOrangeEvent;
import energy.usef.dso.workflow.transformer.DsoMeterEventTransformer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DSO collect orange regime data coordinator.
 */
@Singleton
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class DsoCollectOrangeRegimeDataCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoCollectOrangeRegimeDataCoordinator.class);

    @Inject
    private Config config;
    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;
    @Inject
    private Event<DetermineOrangeEvent> determineOrangeEventManager;
    @Inject
    private JMSHelperService jmsHelperService;
    @Inject
    private WorkflowStepExecuter workflowStepExecuter;
    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * Initiates collect orange regime data process.
     *
     * @param event the {@link InitiateCollectOrangeRegimeDataEvent} event that triggers the process.
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void initiateCollectOrangeRegimeData(@Observes InitiateCollectOrangeRegimeDataEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        LocalDate dayOneMonthBefore = DateTimeUtil.getCurrentDate().minusMonths(1);
        LocalDate startDay = dayOneMonthBefore.withDayOfMonth(1);
        LocalDate endDay = dayOneMonthBefore.dayOfMonth().withMaximumValue();

        List<MeterDataCompany> mdcList = dsoPlanboardBusinessService.findAllMDCs();

        for (LocalDate day = startDay; !day.isAfter(endDay); day = day.plusDays(1)) {
            // Retrieving a list of all connections involved in the orange regime processing.
            Map<ConnectionGroup, List<Connection>> connectionsMap = corePlanboardBusinessService
                    .findConnections(day, day, RegimeType.ORANGE);
            if (!connectionsMap.isEmpty()) {
                // Looping through all the MDCs
                for (MeterDataCompany meterDataCompany : mdcList) {
                    sendMeterDataQuery(connectionsMap, meterDataCompany.getDomain(), day, sequenceGeneratorService.next());
                }
            }
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void sendMeterDataQuery(Map<ConnectionGroup, List<Connection>> connectionMap, String mdcDomain, LocalDate day,
            long sequenceNumber) {
        // Query meter data company (MDC) for connections
        LOGGER.debug("Preparing sending MeterDataQuery to Meter Data Company {} for Day {}", mdcDomain, day);

        MessageMetadata messageMetadata = MessageMetadataBuilder
                .build(mdcDomain, USEFRole.MDC, config.getProperty(ConfigParam.HOST_DOMAIN),
                        USEFRole.DSO, TRANSACTIONAL).build();

        // Filling the MeterDataQuery message
        MeterDataQuery meterDataQuery = new MeterDataQuery();
        meterDataQuery.setMessageMetadata(messageMetadata);
        meterDataQuery.setDateRangeStart(day);
        meterDataQuery.setDateRangeEnd(day);
        meterDataQuery.setQueryType(MeterDataQueryType.EVENTS);
        meterDataQuery.getConnections().addAll(connectionMap.keySet().stream().map(connectionGroup -> {
            Connections mdqConnectionGroup = new Connections();
            mdqConnectionGroup.setParent(connectionGroup.getUsefIdentifier());
            return mdqConnectionGroup;
        }).collect(Collectors.toList()));
        fillMeterDataQueryConnections(meterDataQuery, connectionMap);

        // Storing in PlanboardMessage, no connectionGroup available because query is for the whole grid.
        // the period should be the startDay of the month.
        corePlanboardBusinessService.storePlanboardMessage(
                new PlanboardMessage(DocumentType.METER_DATA_QUERY_EVENTS, sequenceNumber, DocumentStatus.SENT, mdcDomain, day,
                        null, null, null));

        // Sending the message
        jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(meterDataQuery));
    }

    private void fillMeterDataQueryConnections(MeterDataQuery meterDataQuery,
            Map<ConnectionGroup, List<Connection>> connectionMap) {
        Map<String, List<String>> connectionAddressesMap = connectionMap.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getUsefIdentifier(),
                        entry -> entry.getValue()
                                .stream()
                                .map(Connection::getEntityAddress)
                                .collect(Collectors.toList())));
        for (Connections connectionGroup : meterDataQuery.getConnections()) {
            connectionGroup.getConnection().addAll(connectionAddressesMap.get(connectionGroup.getParent()));
        }
    }

    /**
     * Finalizes collect orange regime data process.
     *
     * @param event the {@link FinalizeCollectOrangeRegimeDataEvent} that triggers the process.
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void finalizeCollectOrangeRegimeData(
            @Observes(during = TransactionPhase.AFTER_COMPLETION) FinalizeCollectOrangeRegimeDataEvent event) {
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);

        for (MeterDataSet meterDataPerCongestionPoint : event.getMeterDataSets()) {
            meterDataPerCongestionPoint.getMeterData().forEach(meterData -> saveConnectionMeterEventsFromXml(
                    meterData.getConnectionMeterEvent()));
        }

        // Assumption: One message is sent/received per one day/MDC domain
        processLastResponse(event.getPeriod());
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void saveConnectionMeterEventsFromXml(List<ConnectionMeterEvent> connectionMeterEventList) {
        for (ConnectionMeterEvent connectionMeterEventXmlObject : connectionMeterEventList) {
            LocalDate day = connectionMeterEventXmlObject.getEventDateTime().toLocalDate().toDateMidnight().toLocalDate();
            String entityAddress = connectionMeterEventXmlObject.getEntityAddress();

            // No connection meter event is saved for this day
            if (dsoPlanboardBusinessService.findConnectionForConnectionMeterEventsPeriod(entityAddress, day) == null) {
                Connection connection = corePlanboardBusinessService.findConnection(entityAddress);
                energy.usef.dso.model.ConnectionMeterEvent connectionMeterEventDbObject =
                        new energy.usef.dso.model.ConnectionMeterEvent(connection,
                                DsoMeterEventTransformer.transformToModel(connectionMeterEventXmlObject.getEventType()),
                                connectionMeterEventXmlObject.getEventDateTime(),
                                connectionMeterEventXmlObject.getEventData());
                dsoPlanboardBusinessService.storeConnectionMeterEvent(connectionMeterEventDbObject);
            }
        }
    }

    private void processLastResponse(LocalDate day) {
        if (corePlanboardBusinessService.findPlanboardMessages(DocumentType.METER_DATA_QUERY_EVENTS, day, day,
                DocumentStatus.SENT).isEmpty()) {
            // The last response message is received
            LOGGER.debug("Last {} response message is received for the day {}", DocumentType.METER_DATA_QUERY_EVENTS, day);
            Map<ConnectionGroup, List<Connection>> connectionsMap = corePlanboardBusinessService
                    .findConnections(day, day, RegimeType.ORANGE);
            List<String> connectionList = dsoPlanboardBusinessService
                    .findConnectionsNotRelatedToConnectionMeterEvents(day, connectionsMap.values().stream().flatMap(
                            Collection::stream).distinct().map(Connection::getEntityAddress).collect(Collectors.toList()))
                    .stream()
                    .map(Connection::getEntityAddress)
                    .collect(Collectors.toList());
            if (!connectionList.isEmpty()) {
                // There are connections not related to ConnectionMeterEvents, invoking PBC
                LOGGER.debug(
                        "There are {} connections not related to ConnectionMeterEvents for the day {}, invoking PBC to generate "
                                + "missing ConnectionMeterEvents.",
                        connectionList.size(), day);
                List<ConnectionMeterEventDto> connectionMeterEventDtoList = invokePBC(connectionList, day);
                saveConnectionMeterEventsFromDto(connectionMeterEventDtoList);
            } else {
                LOGGER.debug("All required ConnectionMeterEvents already generated.");
            }

            // Triggering DetermineOrangeEvent
            determineOrangeEventManager.fire(new DetermineOrangeEvent(day, day));
        }
    }

    private void saveConnectionMeterEventsFromDto(List<ConnectionMeterEventDto> connectionMeterEventDtoList) {
        for (ConnectionMeterEventDto connectionMeterEventDto : connectionMeterEventDtoList) {
            Connection connection = corePlanboardBusinessService.findConnection(connectionMeterEventDto.getEntityAddress());
            energy.usef.dso.model.ConnectionMeterEvent connectionMeterEvent =
                    new energy.usef.dso.model.ConnectionMeterEvent(connection,
                            DsoMeterEventTransformer.transformToModel(connectionMeterEventDto.getEventType()),
                            connectionMeterEventDto.getEventDateTime(),
                            connectionMeterEventDto.getEventData());
            dsoPlanboardBusinessService.storeConnectionMeterEvent(connectionMeterEvent);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ConnectionMeterEventDto> invokePBC(List<String> connectionList, LocalDate day) {
        // Preparing context
        DefaultWorkflowContext inputContext = new DefaultWorkflowContext();
        inputContext.setValue(MeterDataQueryEventsParameter.IN.CONNECTION_LIST.name(), connectionList);
        inputContext.setValue(MeterDataQueryEventsParameter.IN.PERIOD.name(), day);

        // Invoking PBC
        WorkflowContext outputContext = workflowStepExecuter.invoke(DsoWorkflowStep.DSO_METER_DATA_QUERY_EVENTS.name(), inputContext);

        // Validating context
        WorkflowUtil.validateContext(DsoWorkflowStep.DSO_METER_DATA_QUERY_EVENTS.name(), outputContext,
                MeterDataQueryEventsParameter.OUT.values());

        return (List<ConnectionMeterEventDto>) outputContext
                .getValue(MeterDataQueryEventsParameter.OUT.CONNECTION_METER_EVENT_DTO_LIST.name());
    }
}
