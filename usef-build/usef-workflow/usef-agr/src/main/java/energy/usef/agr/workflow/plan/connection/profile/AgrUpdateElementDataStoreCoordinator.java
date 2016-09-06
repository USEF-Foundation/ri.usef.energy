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

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.model.Element;
import energy.usef.agr.service.business.AgrElementBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.transformer.ElementTransformer;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.util.List;

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
 * Coordinator class in charge of the workflow updating the element data store.
 */
@Singleton
public class AgrUpdateElementDataStoreCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrUpdateElementDataStoreCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private Config config;

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Inject
    private AgrElementBusinessService agrElementBusinessService;

    @Inject
    private Event<CreateConnectionProfileEvent> createConnectionProfileEventManager;

    @Inject
    private EventValidationService eventValidationService;


    /**
     * Update the element data store in order to supply up-to-date data to the subsequent Portfolio initialization process.
     *
     * @param event {@link AgrUpdateElementDataStoreEvent} event triggering the workflow.
     */
    @Lock(LockType.WRITE)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateElementDataStore(@Observes(during = TransactionPhase.AFTER_COMPLETION) AgrUpdateElementDataStoreEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodInFuture(event);

        // retrieve the input for the PBC from the database
        List<ConnectionPortfolioDto> connectionPortfolioDtoList = agrPortfolioBusinessService
                .findConnectionPortfolioDto(event.getPeriod());

        List<ElementDto> elementDtoList = invokePBC(event.getPeriod(), connectionPortfolioDtoList);

        // persist the elements
        List<Element> elementList = ElementTransformer.transformToModelList(elementDtoList);
        agrElementBusinessService.createElements(elementList);

        createConnectionProfileEventManager.fire(new CreateConnectionProfileEvent(event.getPeriod()));

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    @SuppressWarnings("unchecked")
    private List<ElementDto> invokePBC(LocalDate period, List<ConnectionPortfolioDto> connectionPortfolioDtoList) {
        // setup the input for the PBC
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(AgrUpdateElementDataStoreParameter.IN.PERIOD.name(), period);
        context.setValue(AgrUpdateElementDataStoreParameter.IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        context.setValue(AgrUpdateElementDataStoreParameter.IN.CONNECTION_PORTFOLIO_LIST.name(), connectionPortfolioDtoList);

        context = workflowStepExecuter.invoke(AgrWorkflowStep.AGR_UPDATE_ELEMENT_DATA_STORE.name(), context);

        // validate output of the PBC
        WorkflowUtil.validateContext(AgrWorkflowStep.AGR_UPDATE_ELEMENT_DATA_STORE.name(), context, AgrUpdateElementDataStoreParameter.OUT
                .values());

        return context.get(AgrUpdateElementDataStoreParameter.OUT.ELEMENT_LIST.name(), List.class);
    }
}
