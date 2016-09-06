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

package energy.usef.agr.workflow.plan.connection.profile;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.service.business.AgrElementBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.plan.connection.profile.CreateConnectionProfileStepParameter.IN;
import energy.usef.agr.workflow.plan.connection.profile.CreateConnectionProfileStepParameter.OUT;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.constant.USEFConstants;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;

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

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class in charge of the workflow populating the Profile power values of the connection portfolio.
 */
@Singleton
public class AgrCreateConnectionProfileCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrCreateConnectionProfileCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private Config config;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private AgrElementBusinessService agrElementBusinessService;

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Inject
    private Event<CreateUdiEvent> createUdiEventManager;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * Create the connection profile power values for the connection portfolio for the period starting the next day and ending
     * {@link ConfigAgrParam#AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL} later.
     *
     * @param createConnectionProfileEvent {@link CreateConnectionProfileEvent} event triggering the workflow.
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    @SuppressWarnings("unchecked")
    public void createConnectionProfile(
            @Observes(during = TransactionPhase.AFTER_COMPLETION) CreateConnectionProfileEvent createConnectionProfileEvent) throws BusinessValidationException {
        LOGGER.info(USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT, createConnectionProfileEvent);
        eventValidationService.validateEventPeriodInFuture(createConnectionProfileEvent);

        LocalDate initializationDate = createConnectionProfileEvent.getPeriod();
        Integer ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);
        Integer initializeDaysInterval = configAgr.getIntegerProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL);
        for (int i = 0; i < initializeDaysInterval; ++i) {
            LocalDate period = initializationDate.plusDays(i);

            List<ConnectionPortfolioDto> connectionPortfolioDto = agrPortfolioBusinessService.findConnectionPortfolioDto(period);

            if (connectionPortfolioDto.isEmpty()) {
                LOGGER.debug("No connections in the portfolio for period [{}]. Workflow will proceed for next periods.", period);
                continue;
            }

            Map<String, List<ElementDto>> elementsPerConnection = agrElementBusinessService.findElementDtos(period).stream().collect(
                    Collectors.groupingBy(ElementDto::getConnectionEntityAddress));

            List<ConnectionPortfolioDto> connectionPortfolioDTOs = invokeCreateConnectionProfilePbc(ptuDuration, period,
                    connectionPortfolioDto, elementsPerConnection);

            agrPortfolioBusinessService.createConnectionProfiles(period, connectionPortfolioDTOs);
        }

        if(!configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            createUdiEventManager.fire(new CreateUdiEvent(createConnectionProfileEvent.getPeriod()));
        }
        LOGGER.info(USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT, createConnectionProfileEvent);
    }

    private List<ConnectionPortfolioDto> invokeCreateConnectionProfilePbc(Integer ptuDuration, LocalDate period,
            List<ConnectionPortfolioDto> connectionPortfolioDto, Map<String, List<ElementDto>> elementsPerConnection) {
        WorkflowContext inputContext = new DefaultWorkflowContext();
        inputContext.setValue(IN.PERIOD.name(), period);
        inputContext.setValue(IN.PTU_DURATION.name(), ptuDuration);
        inputContext.setValue(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), connectionPortfolioDto);
        inputContext.setValue(IN.ELEMENT_PER_CONNECTION_MAP.name(), elementsPerConnection);

        WorkflowContext outContext = workflowStepExecuter.invoke(AgrWorkflowStep.AGR_CREATE_CONNECTION_PROFILE.name(), inputContext);
        WorkflowUtil.validateContext(AgrWorkflowStep.AGR_CREATE_CONNECTION_PROFILE.name(), outContext, OUT.values());

        return (List<ConnectionPortfolioDto>) outContext.getValue(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name());
    }

}
