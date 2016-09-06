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

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_CREATE_UDI;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.service.business.AgrDeviceCapabilityBusinessService;
import energy.usef.agr.service.business.AgrElementBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.plan.connection.profile.CreateUdiStepParameter.IN;
import energy.usef.agr.workflow.plan.connection.profile.CreateUdiStepParameter.OUT;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.constant.USEFConstants;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class in charge of the workflow populating the Profile power values of the connection portfolio.
 */
@Singleton
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class AgrCreateUdiCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrCreateUdiCoordinator.class);

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
    private AgrDeviceCapabilityBusinessService agrDeviceCapabilityBusinessService;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * Create the Udi's for the active connection Profile.
     *
     * @param createUdiEvent {@link CreateUdiEvent} event triggering the workflow.
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    @SuppressWarnings("unchecked")
    public void createUdis(@Observes(during = TransactionPhase.AFTER_COMPLETION) CreateUdiEvent createUdiEvent) throws BusinessValidationException {
        LOGGER.info(USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT, createUdiEvent);
        eventValidationService.validateEventPeriodInFuture(createUdiEvent);

        LocalDate initializationDate = createUdiEvent.getPeriod();
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

            WorkflowContext resultContext = invokeCreateUdiPbc(ptuDuration, period, connectionPortfolioDto, elementsPerConnection);

            List<ConnectionPortfolioDto> connectionPortfolioDTOList = (List<ConnectionPortfolioDto>) resultContext
                    .getValue(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name());
            Map<String, List<UdiEventDto>> udiEvents = (Map<String, List<UdiEventDto>>) resultContext
                    .getValue(OUT.UDI_EVENTS_PER_UDI_MAP.name());

            agrPortfolioBusinessService.createUdis(period, connectionPortfolioDTOList);

            List<UdiEventDto> udiEventDtos = udiEvents.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
            agrDeviceCapabilityBusinessService.updateUdiEvents(period, udiEventDtos);
        }
        LOGGER.info(USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT, createUdiEvent);
    }

    private WorkflowContext invokeCreateUdiPbc(Integer ptuDuration, LocalDate period,
            List<ConnectionPortfolioDto> connectionPortfolioDto, Map<String, List<ElementDto>> elementsPerConnection) {
        WorkflowContext inputContext = new DefaultWorkflowContext();
        inputContext.setValue(IN.PERIOD.name(), period);
        inputContext.setValue(IN.PTU_DURATION.name(), ptuDuration);
        inputContext.setValue(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), connectionPortfolioDto);
        inputContext.setValue(IN.ELEMENT_PER_CONNECTION_MAP.name(), elementsPerConnection);

        WorkflowContext outContext = workflowStepExecuter.invoke(AGR_CREATE_UDI.name(), inputContext);
        WorkflowUtil.validateContext(AGR_CREATE_UDI.name(), outContext, OUT.values());

        return outContext;
    }

}
