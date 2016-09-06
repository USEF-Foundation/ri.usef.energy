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

package energy.usef.agr.workflow.operate.netdemand;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_DETERMINE_NET_DEMANDS;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.service.business.AgrDeviceCapabilityBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.transformer.UdiEventTransformer;
import energy.usef.agr.workflow.operate.netdemand.DetermineNetDemandStepParameter.IN;
import energy.usef.agr.workflow.operate.netdemand.DetermineNetDemandStepParameter.OUT;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.validation.CoreBusinessError;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This coordinater executes the business logic for Creating and Sending FlexOffer's.
 */
@Singleton
@Transactional(value = TxType.REQUIRES_NEW)
public class AgrDetermineNetDemandCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrDetermineNetDemandCoordinator.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Inject
    private AgrDeviceCapabilityBusinessService agrDeviceCapabilityBusinessService;
    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private Config config;

    /**
     * Handles a {@link DetermineNetDemandEvent}.
     *
     * @param event
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void handleEvent(@Observes DetermineNetDemandEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        invokeDetermineNetDemand(event);
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    @SuppressWarnings("unchecked")
    private void invokeDetermineNetDemand(DetermineNetDemandEvent event) {
        // periods for of the initialized planboard
        List<LocalDate> daysOfPlanboard = corePlanboardBusinessService.findInitializedDaysOfPlanboard();
        if (daysOfPlanboard == null || daysOfPlanboard.isEmpty()) {
            LOGGER.error(CoreBusinessError.NOT_INITIALIZED_PLANBOARD.getError(), "PRESENT/FUTURE");
            LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
            return;
        }
        // fetch portfolio for each initialized day
        Map<LocalDate, List<ConnectionPortfolioDto>> connectionPortfolioPerPeriod = agrPortfolioBusinessService
                .findConnectionPortfolioDto(daysOfPlanboard.get(0), daysOfPlanboard.get(daysOfPlanboard.size() - 1));
        // for each initialized day, call determine net demand and update device capabilities and portfolio, unless empy
        for (LocalDate period : daysOfPlanboard) {
            // transform and execute step
            List<ConnectionPortfolioDto> portfolio = connectionPortfolioPerPeriod.get(period);
            if (portfolio == null || portfolio.isEmpty()) {
                // skip grid point if there are no connections.
                LOGGER.info("No ConnectionPortfolio found for period [{}], no reason to determine net demands.", period);
                continue;
            }
            LOGGER.debug("Determine Net Demand for {} connections", portfolio.size());
            WorkflowContext outContext = executeDetermineNetDemandStep(period, portfolio);
            List<ConnectionPortfolioDto> updatedConnectionPortfolioDTO = outContext.get(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(),
                    List.class);
            List<UdiEventDto> updatedUdiEvents = outContext.get(OUT.UPDATED_UDI_EVENT_DTO_LIST.name(), List.class);
            // process portfolio data
            agrPortfolioBusinessService.updateConnectionPortfolio(period, updatedConnectionPortfolioDTO);
            agrDeviceCapabilityBusinessService.updateUdiEvents(period, updatedUdiEvents);
        }

    }

    private WorkflowContext executeDetermineNetDemandStep(LocalDate period, List<ConnectionPortfolioDto> portfolio) {

        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        inContext.setValue(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), portfolio);
        inContext.setValue(IN.UDI_EVENT_DTO_MAP.name(), buildUdiEventDtoMap(period));
        inContext.setValue(IN.PERIOD.name(), period);

        WorkflowContext outContext = workflowStepExecuter.invoke(AGR_DETERMINE_NET_DEMANDS.name(), inContext);
        WorkflowUtil.validateContext(AGR_DETERMINE_NET_DEMANDS.name(), outContext, OUT.values());
        return outContext;
    }

    /**
     * Maps the udi events per udi endpoint per connection entity address.
     *
     * @param period {@link LocalDate} period.
     * @return a {@link Map}.
     */
    private Map<String, Map<String, List<UdiEventDto>>> buildUdiEventDtoMap(LocalDate period) {
        return agrPortfolioBusinessService.findUdiEventsForPeriod(period)
                .stream()
                .collect(groupingBy(udiEvent -> udiEvent.getUdi().getConnection().getEntityAddress(),
                        groupingBy(udiEvent -> udiEvent.getUdi().getEndpoint(),
                                mapping(UdiEventTransformer::transformToDto, toList()))));
    }
}
