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

package energy.usef.agr.workflow.operate.reoptimize;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.model.Udi;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.transformer.UdiEventTransformer;
import energy.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyEvent;
import energy.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesEvent;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.BackToPlanEvent;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.FlexOrderTransformer;
import energy.usef.core.workflow.transformer.PrognosisTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator for workflow: re-optimize aggregator portfolio. This coordinator is stateless because it can be executed for
 * different days simultaneous.
 */
@Stateless
public class AgrReOptimizePortfolioCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrReOptimizePortfolioCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Inject
    private AgrPlanboardBusinessService agrPlanboardBusinessService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private Event<ExecuteReOptimizePortfolioEvent> executeReOptimizePortfolioEventEventManager;

    @Inject
    private Event<ReCreatePrognosesEvent> reCreatePrognosesEventManager;

    @Inject
    private Event<BackToPlanEvent> backToPlanEventManager;

    @Inject
    private Config config;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private ReOptimizeFlagHolder reOptimizeFlagHolder;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * The {@link ReOptimizePortfolioEvent} is handled here and used in the other parts of the application.
     *
     * @param event the {@link ReOptimizePortfolioEvent} triggering the process.
     */
    @Asynchronous
    public void trigger(@Observes(during = TransactionPhase.AFTER_COMPLETION) ReOptimizePortfolioEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);
        if (reOptimizeFlagHolder.isRunning(event.getPeriod())) {
            reOptimizeFlagHolder.setToBeReoptimized(event.getPeriod(), true);
        } else {
            executeReOptimizePortfolioEventEventManager.fire(new ExecuteReOptimizePortfolioEvent(event.getPeriod()));
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Handles the event fired to actually execute the connection portfolio reoptimization.
     *
     * @param event {@link ExecuteReOptimizePortfolioEvent} to handle.
     */
    @Asynchronous
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(@Observes(during = TransactionPhase.AFTER_COMPLETION) ExecuteReOptimizePortfolioEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        reOptimizeFlagHolder.setIsRunning(event.getPtuDate(), true);

        reOptimizePortfolio(event);

        if (reOptimizeFlagHolder.toBeReoptimized(event.getPtuDate())) {
            reOptimizeFlagHolder.setToBeReoptimized(event.getPtuDate(), false);
            executeReOptimizePortfolioEventEventManager.fire(new ExecuteReOptimizePortfolioEvent(event.getPtuDate()));
        } else {
            reOptimizeFlagHolder.setIsRunning(event.getPtuDate(), false);
            // Firing event to trigger ReCreatePrognoses workflow.
            reCreatePrognosesEventManager.fire(new ReCreatePrognosesEvent(event.getPtuDate()));
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * This method coordinates the workflow of the re-optimization of portfolios, it fetches the latest connection portfolio DTO and
     * passes it to the Pluggable Business Component. The PBC returns a list of UDI control messages, DeviceMessages, which it
     * passes as an argument for the ControlActiveDemandSupplyEvent.
     *
     * @param event A {@link ExecuteReOptimizePortfolioEvent}
     */
    private void reOptimizePortfolio(ExecuteReOptimizePortfolioEvent event) {
        LocalDate ptuDate = event.getPtuDate();

        // Getting Connection Portfolio
        List<ConnectionPortfolioDto> connectionPortfolioDTOs = agrPortfolioBusinessService.findConnectionPortfolioDto(ptuDate);

        if (connectionPortfolioDTOs.isEmpty()) {
            LOGGER.warn("Empty Connection Portfolio");
        }

        // Processing Connection Portfolio
        reOptimizeConnectionGroupPortfolio(connectionPortfolioDTOs, ptuDate);
    }

    @SuppressWarnings("unchecked")
    private ControlActiveDemandSupplyEvent reOptimizeConnectionGroupPortfolio(List<ConnectionPortfolioDto> connectionPortfolioDTOs,
            LocalDate period) {

        // gather all the data that is needed for the re-optimalization of the portfolio
        List<PrognosisDto> latestAPlans = findLastPrognoses(period, PrognosisType.A_PLAN);
        List<PrognosisDto> latestDPrognoses = findLastPrognoses(period, PrognosisType.D_PROGNOSIS);
        List<PtuFlexOrder> ptuFlexOrderList = corePlanboardBusinessService.findAcceptedFlexOrdersForUsefIdentifierOnDate(
                Optional.empty(), period);
        List<PlanboardMessage> acceptedFlexOrders = corePlanboardBusinessService.findPlanboardMessagesForConnectionGroup(null, null,
                DocumentType.FLEX_ORDER, period, DocumentStatus.ACCEPTED);
        List<FlexOrderDto> flexOrderDtoList = transformPtuFlexOrderListToDtoList(ptuFlexOrderList);
        Map<String, Map<Long, Map<Integer, PtuFlexOffer>>> ptuFlexOffersMap = findFlexOffersForOrders(period);
        List<PrognosisDto> relatedPrognosis = findPrognosesForOrders(period);

        fetchPowerAndPrices(flexOrderDtoList, ptuFlexOffersMap);

        //invoke the PBC
        WorkflowContext resultContext = invokePBC(connectionPortfolioDTOs, latestDPrognoses, latestAPlans, relatedPrognosis,
                flexOrderDtoList, period);

        //update the connectionPortfolio
        List<ConnectionPortfolioDto> reoptimizedPortfolio = resultContext.get(ReOptimizePortfolioStepParameter.OUT.CONNECTION_PORTFOLIO_OUT.name(), List.class);

        List<DeviceMessageDto> deviceMessageDtos = resultContext.get(ReOptimizePortfolioStepParameter.OUT.DEVICE_MESSAGES_OUT.name(), List.class);
        if (deviceMessageDtos != null && !deviceMessageDtos.isEmpty()) {
            Map<String, Udi> udis = agrPortfolioBusinessService.findActiveUdisMappedPerEndpoint(period);
            agrPortfolioBusinessService.storeDeviceMessages(deviceMessageDtos, udis);
        }

        agrPortfolioBusinessService.updateConnectionPortfolio(period, reoptimizedPortfolio);

        // Change status of all flex orders to processed
        for (PtuFlexOrder ptuFlexorder : ptuFlexOrderList) {
            agrPlanboardBusinessService.changeStatusOfPtuFlexOrder(ptuFlexorder, AcknowledgementStatus.PROCESSED);
        }
        for (PlanboardMessage flexOrder : acceptedFlexOrders) {
            flexOrder.setDocumentStatus(DocumentStatus.PROCESSED);
        }
        // Scheduling realize A-plan / D-Prognosis by controlling ADS (UC1021)
        return new ControlActiveDemandSupplyEvent();
    }

    private List<FlexOrderDto> transformPtuFlexOrderListToDtoList(List<PtuFlexOrder> ptuFlexOrderList) {
        return ptuFlexOrderList.stream()
                .collect(groupingBy(ptuFlexOrder -> ptuFlexOrder.getParticipantDomain() + ptuFlexOrder.getSequence()
                        + ptuFlexOrder.getConnectionGroup().getUsefIdentifier()))
                .values()
                .stream()
                .map(FlexOrderTransformer::transformPtuFlexOrders)
                .collect(Collectors.toList());
    }

    private void fetchPowerAndPrices(List<FlexOrderDto> flexOrderDtoList,
            Map<String, Map<Long, Map<Integer, PtuFlexOffer>>> ptuFlexOffersMap) {
        // Fetch the power and price of the related flex offer
        for (FlexOrderDto flexOrderDto : flexOrderDtoList) {
            //add Power value to PtuFlexOrderDto
            flexOrderDto.getPtus().stream().forEach(ptuFlexOrder -> {
                PtuFlexOffer ptuFlexOffer = ptuFlexOffersMap.get(flexOrderDto.getParticipantDomain())
                        .get(flexOrderDto.getFlexOfferSequenceNumber())
                        .get(ptuFlexOrder.getPtuIndex().intValue());
                ptuFlexOrder.setPower(ptuFlexOffer.getPower());
                ptuFlexOrder.setPrice(ptuFlexOffer.getPrice());
            });
        }
    }

    private List<PrognosisDto> findLastPrognoses(LocalDate period, PrognosisType prognosisType) {
        return corePlanboardBusinessService.findLastPrognoses(period, prognosisType)
                .stream()
                .collect(groupingBy(ptuPrognosis -> "" + ptuPrognosis.getParticipantDomain() + ptuPrognosis.getConnectionGroup()
                        .getUsefIdentifier())).values().stream().map(PrognosisTransformer::mapToPrognosis)
                .collect(Collectors.toList());
    }

    // participantDomain -> sequenceNumber -> ptuIndex -> PtuFlexOffer
    private Map<String, Map<Long, Map<Integer, PtuFlexOffer>>> findFlexOffersForOrders(LocalDate period) {
        return corePlanboardBusinessService.findFlexOffersWithOrderInPeriod(period)
                .stream()
                .collect(groupingBy(PtuFlexOffer::getParticipantDomain, groupingBy(PtuFlexOffer::getSequence,
                        toMap(ptuFlexOffer -> ptuFlexOffer.getPtuContainer().getPtuIndex(), Function.identity()))));
    }

    private List<PrognosisDto> findPrognosesForOrders(LocalDate period) {
        List<PtuPrognosis> prognosesWithOrderInPeriod = corePlanboardBusinessService.findPrognosesWithOrderInPeriod(period);
        Map<String, Map<Long, List<PtuPrognosis>>> ptuPrognosesPerSequencePerParticipant = prognosesWithOrderInPeriod.stream()
                .collect(groupingBy(PtuPrognosis::getParticipantDomain, groupingBy(PtuPrognosis::getSequence)));
        List<PrognosisDto> result = new ArrayList<>();
        for (String participantDomain : ptuPrognosesPerSequencePerParticipant.keySet()) {
            result.addAll(ptuPrognosesPerSequencePerParticipant.get(participantDomain)
                    .keySet()
                    .stream()
                    .map(sequenceNumber -> PrognosisTransformer.mapToPrognosis(
                            ptuPrognosesPerSequencePerParticipant.get(participantDomain).get(sequenceNumber)))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    private WorkflowContext invokePBC(List<ConnectionPortfolioDto> connectionPortfolioDTOs, List<PrognosisDto> latestDPrognoses,
            List<PrognosisDto> latestAPlans, List<PrognosisDto> relatedPrognosis, List<FlexOrderDto> flexOrderDtoList,
            LocalDate ptuDate) {

        WorkflowContext workflowContextIn = new DefaultWorkflowContext();
        workflowContextIn.setValue(ReOptimizePortfolioStepParameter.IN.CONNECTION_PORTFOLIO_IN.name(), connectionPortfolioDTOs);
        workflowContextIn.setValue(ReOptimizePortfolioStepParameter.IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(),
                corePlanboardBusinessService.buildConnectionGroupsToConnectionsMap(ptuDate));

        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);
        int ptuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime(), ptuDuration);
        workflowContextIn.setValue(ReOptimizePortfolioStepParameter.IN.PTU_DURATION.name(), ptuDuration);
        workflowContextIn.setValue(ReOptimizePortfolioStepParameter.IN.PTU_DATE.name(), ptuDate);
        workflowContextIn.setValue(ReOptimizePortfolioStepParameter.IN.CURRENT_PTU_INDEX.name(), ptuIndex);

        // RELEVANT_PROGNOSIS_LIST contains all prognosis messages that are relevant to the FlexOrder messages for the period.
        workflowContextIn.setValue(ReOptimizePortfolioStepParameter.IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), latestDPrognoses);
        workflowContextIn.setValue(ReOptimizePortfolioStepParameter.IN.LATEST_A_PLAN_DTO_LIST.name(), latestAPlans);
        workflowContextIn.setValue(ReOptimizePortfolioStepParameter.IN.RELEVANT_PROGNOSIS_LIST.name(), relatedPrognosis);
        workflowContextIn.setValue(ReOptimizePortfolioStepParameter.IN.RECEIVED_FLEXORDER_LIST.name(), flexOrderDtoList);

        // Send portfolio to PBC for re-optimization && receive UDI control messages on workflowContext
        WorkflowContext workflowContextOut;
        if (configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            workflowContextOut = workflowStepExecuter.invoke(AgrWorkflowStep.AGR_NON_UDI_REOPTIMIZE_PORTFOLIO.name(), workflowContextIn);
            WorkflowUtil.validateContext(AgrWorkflowStep.AGR_NON_UDI_REOPTIMIZE_PORTFOLIO.name(), workflowContextOut, ReOptimizePortfolioStepParameter.OUT_NON_UDI
                    .values());
        } else {
            workflowContextIn.setValue(ReOptimizePortfolioStepParameter.IN.UDI_EVENTS.name(), fetchUdiEventDtos(ptuDate));
            workflowContextOut = workflowStepExecuter.invoke(AgrWorkflowStep.AGR_REOPTIMIZE_PORTFOLIO.name(), workflowContextIn);
            WorkflowUtil.validateContext(AgrWorkflowStep.AGR_REOPTIMIZE_PORTFOLIO.name(), workflowContextOut, ReOptimizePortfolioStepParameter.OUT
                    .values());
        }

        return workflowContextOut;

    }

    private List<UdiEventDto> fetchUdiEventDtos(LocalDate period) {
        return agrPortfolioBusinessService.findUdiEventsForPeriod(period)
                .stream()
                .map(UdiEventTransformer::transformToDto)
                .collect(Collectors.toList());
    }
}
