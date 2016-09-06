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

package energy.usef.agr.workflow.settlement.initiate;

import static energy.usef.core.workflow.settlement.CoreInitiateSettlementParameter.IN;
import static energy.usef.core.workflow.settlement.CoreInitiateSettlementParameter.IN.FLEX_OFFER_DTO_LIST;
import static java.util.stream.Collectors.toMap;

import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.constant.USEFConstants;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.coordinator.AbstractSettlementCoordinator;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.SettlementDto;
import energy.usef.core.workflow.dto.USEFRoleDto;
import energy.usef.core.workflow.settlement.CoreInitiateSettlementParameter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator for the aggregator to initiate the settlement process.
 */
@Stateless
@LocalBean
public class AgrInitiateSettlementCoordinator extends AbstractSettlementCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrInitiateSettlementCoordinator.class);

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    /**
     * Handles the event triggering the initiation of the settlement.
     *
     * @param initiateSettlementEvent {@link InitiateSettlementEvent}.
     */
    public void handleAgrInitiateSettlementEvent(@Observes InitiateSettlementEvent initiateSettlementEvent) {
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT, initiateSettlementEvent);
        WorkflowContext inContext = initiateWorkflowContext(
                initiateSettlementEvent.getPeriodInMonth().withDayOfMonth(1),
                initiateSettlementEvent.getPeriodInMonth().plusMonths(1).withDayOfMonth(1).minusDays(1));
        SettlementDto settlementDto = invokeInitiateSettlementPbc(inContext);

        // Add the settlement prices to the SettlementDto
        settlementDto = calculateSettlementPrice(settlementDto, inContext.get(FLEX_OFFER_DTO_LIST.name(), List.class));

        saveSettlement(settlementDto);
        LOGGER.debug(USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT, initiateSettlementEvent);
    }

    /**
     * Invokes the Pluggable Business Component in charge of the initialization of the settlement. Outcoming workflow context will
     * be validated and settlement items will be save in the database.
     *
     * @param inContext {@link WorkflowContext} input context.
     */
    @Override
    public SettlementDto invokeInitiateSettlementPbc(WorkflowContext inContext) {
        WorkflowContext outContext = workflowStepExecuter.invoke(getWorkflowName(), inContext);
        validateWorkflowContext(outContext);
        return outContext.get(CoreInitiateSettlementParameter.OUT.SETTLEMENT_DTO.name(), SettlementDto.class);
    }

    /**
     * Gets the workflow name which will be used to invoke the PBC.
     *
     * @return
     */
    @Override
    public String getWorkflowName() {
        return AgrWorkflowStep.AGR_INITIATE_SETTLEMENT.name();
    }

    @Override
    public WorkflowContext initiateWorkflowContext(LocalDate startDate, LocalDate endDate) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(IN.START_DATE.name(), startDate);
        inContext.setValue(IN.END_DATE.name(), endDate);
        inContext.setValue(IN.PROGNOSIS_DTO_LIST.name(), fetchRelevantPrognoses(startDate, endDate));
        inContext.setValue(IN.FLEX_REQUEST_DTO_LIST.name(), fetchRelevantFlexRequests(startDate, endDate));
        List<FlexOfferDto> flexOfferDtos = fetchRelevantFlexOffers(startDate, endDate);
        inContext.setValue(IN.FLEX_OFFER_DTO_LIST.name(), flexOfferDtos);
        inContext.setValue(IN.FLEX_ORDER_DTO_LIST.name(), fetchRelevantFlexOrders(startDate, endDate, flexOfferDtos));
        inContext.setValue(AgrInitiateSettlementParameter.IN.CONNECTION_PORTFOLIO_DTO_LIST.name(),
                fetchConnectionPortfolio(startDate, endDate));
        inContext.setValue(AgrInitiateSettlementParameter.IN.CONNECTION_GROUP_PORTFOLIO_DTO_PER_PERIOD_MAP.name(),
                fetchConnectionGroupPortfolio(startDate, endDate));
        Map<ConnectionGroup, Set<Connection>> activeConnectionGroups = coreSettlementBusinessService
                .findActiveConnectionGroupsWithConnections(startDate, endDate);
        inContext.setValue(AgrInitiateSettlementParameter.IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(),
                buildConnectionGroupsToConnectionsMap(activeConnectionGroups));
        inContext.setValue(AgrInitiateSettlementParameter.IN.CONNECTION_GROUPS_TO_USEF_ROLE_MAP.name(),
                buildConnectionGroupsToUsefRole(activeConnectionGroups));
        inContext.setValue(IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        return inContext;
    }

    private Map<LocalDate, List<ConnectionPortfolioDto>> fetchConnectionPortfolio(LocalDate startDate, LocalDate endDate) {
        return agrPortfolioBusinessService.findConnectionPortfolioDto(startDate, endDate);
    }

    private Map<String, List<String>> buildConnectionGroupsToConnectionsMap(
            Map<ConnectionGroup, Set<Connection>> connectionGroupWithConnections) {
        return connectionGroupWithConnections
                .entrySet()
                .stream()
                .collect(toMap(entry -> entry.getKey().getUsefIdentifier(),
                        entry -> entry.getValue().stream().map(Connection::getEntityAddress).collect(Collectors.toList())));
    }

    private Map<String, USEFRoleDto> buildConnectionGroupsToUsefRole(
            Map<ConnectionGroup, Set<Connection>> connectionGroupWithConnections) {
        return connectionGroupWithConnections.keySet()
                .stream()
                .collect(toMap(ConnectionGroup::getUsefIdentifier, connectionGroup -> {
                    if (connectionGroup instanceof CongestionPointConnectionGroup) {
                        return USEFRoleDto.DSO;
                    } else {
                        return USEFRoleDto.BRP;
                    }
                }));
    }

    private Map<LocalDate, List<ConnectionGroupPortfolioDto>> fetchConnectionGroupPortfolio(LocalDate startDate,
            LocalDate endDate) {
        return DateTimeUtil.generateDatesOfInterval(startDate, endDate)
                .stream()
                .collect(toMap(Function.identity(),
                        period -> agrPortfolioBusinessService.findConnectionGroupPortfolioDto(period, Optional.empty())));
    }

}
