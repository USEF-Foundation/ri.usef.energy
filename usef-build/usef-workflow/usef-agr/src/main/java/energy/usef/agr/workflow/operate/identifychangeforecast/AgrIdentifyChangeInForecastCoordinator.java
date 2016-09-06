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

package energy.usef.agr.workflow.operate.identifychangeforecast;

import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.operate.deviation.DetectDeviationEvent;
import energy.usef.agr.workflow.operate.identifychangeforecast.IdentifyChangeInForecastStepParameter.OUT;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.dto.PtuContainerDto;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.validation.CoreBusinessError;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_IDENTIFY_CHANGE_IN_FORECAST;
import static energy.usef.agr.workflow.operate.identifychangeforecast.IdentifyChangeInForecastStepParameter.IN;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

/**
 * Aggregator coordinator identifying changes in the connection forecast in the 'AGR identify changes in forecast' workflow.
 */
@Singleton
public class AgrIdentifyChangeInForecastCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrIdentifyChangeInForecastCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Inject
    private Config config;

    @Inject
    private Event<DetectDeviationEvent> detectDeviationEventManager;

    @Inject
    private Event<ReOptimizePortfolioEvent> reOptimizePortfolioEventManager;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private CorePlanboardValidatorService corePlanboardValidatorService;

    /**
     * Handles IdentifyChangeInForecastEvent event for the aggregator.
     * <p>
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Asynchronous
    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void identifyChangesInForecast(@Observes IdentifyChangeInForecastEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        // duration of the initialized planboard
        List<LocalDate> daysOfPlanboard = corePlanboardBusinessService.findInitializedDaysOfPlanboard();
        if (daysOfPlanboard == null || daysOfPlanboard.isEmpty()) {
            LOGGER.error(CoreBusinessError.NOT_INITIALIZED_PLANBOARD.getError(), "PRESENT/FUTURE");
            LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
            return;
        }
        for (LocalDate period : daysOfPlanboard) {
            WorkflowContext resultContext = invokeIdentifyChangesInForecastPbc(period);

            if (!resultContext.get(OUT.FORECAST_CHANGED.name(), Boolean.class)) {
                LOGGER.info("No Connection Forecast change identified.");
                LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
                continue;
            }
            LOGGER.info("Connection Forecast change identified,the next steps will be determined.");
            List<PtuContainerDto> changedPtus = resultContext.get(OUT.FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST.name(), List.class);
            triggerNextSteps(period, changedPtus);
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private WorkflowContext invokeIdentifyChangesInForecastPbc(LocalDate period) {
        WorkflowContext inContext = new DefaultWorkflowContext();

        inContext.setValue(IN.CONNECTION_PORTFOLIO.name(), agrPortfolioBusinessService.findConnectionPortfolioDto(period));
        inContext.setValue(IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        inContext.setValue(IN.PERIOD.name(), period);

        WorkflowContext outContext = workflowStepExecuter.invoke(AGR_IDENTIFY_CHANGE_IN_FORECAST.name(), inContext);

        // Validating outgoing context
        WorkflowUtil.validateContext(AGR_IDENTIFY_CHANGE_IN_FORECAST.name(), outContext, OUT.values());
        return outContext;
    }

    private void triggerNextSteps(LocalDate period, List<PtuContainerDto> changedPtus) {
        final LocalDate today = DateTimeUtil.getCurrentDate();
        if (today.isEqual(period)) {
            List<PtuContainerDto> changedPtusForToday = changedPtus.stream()
                    .filter(changedPtu -> today.equals(changedPtu.getPtuDate()))
                    .collect(Collectors.toList());
            triggerNextStepsForToday(today, changedPtusForToday);
        } else if (changedPtus.stream().map(PtuContainerDto::getPtuDate).distinct().anyMatch(period::isEqual)) {
            reOptimizePortfolioEventManager.fire(new ReOptimizePortfolioEvent(period));
        }
    }

    private void triggerNextStepsForToday(LocalDate today, List<PtuContainerDto> changedPtusForToday) {
        int currentPtu = PtuUtil
                .getPtuIndex(DateTimeUtil.getCurrentDateTime(), config.getIntegerProperty(ConfigParam.PTU_DURATION));

        boolean changedPtuInOperate = false;
        boolean changedPtuAfterIntraDayGateClosure = false;
        for (PtuContainerDto ptuContainerDto : changedPtusForToday) {
            //skip current and past ptus.
            if (ptuContainerDto.getPtuIndex() <= currentPtu) {
                continue;
            }
            PtuContainer testPtuContainer = new PtuContainer(today, ptuContainerDto.getPtuIndex());
            if (corePlanboardValidatorService.isPtuContainerWithinIntradayGateClosureTime(testPtuContainer)) {
                changedPtuInOperate = true;
            } else {
                changedPtuAfterIntraDayGateClosure = true;
            }
        }

        if (changedPtuInOperate) {
            fireDetectDeviationEvents(today);
        } else if (changedPtuAfterIntraDayGateClosure) {
            reOptimizePortfolioEventManager.fire(new ReOptimizePortfolioEvent(today));
        }
    }

    private void fireDetectDeviationEvents(LocalDate period) {
        detectDeviationEventManager.fire(new DetectDeviationEvent(period));
    }

}
