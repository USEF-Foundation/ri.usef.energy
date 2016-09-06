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

package energy.usef.dso.workflow.operate;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;
import static energy.usef.dso.workflow.DsoWorkflowStep.DSO_LIMIT_CONNECTIONS;
import static energy.usef.dso.workflow.DsoWorkflowStep.DSO_MONITOR_GRID;
import static energy.usef.dso.workflow.DsoWorkflowStep.DSO_PLACE_OPERATE_FLEX_ORDERS;
import static energy.usef.dso.workflow.DsoWorkflowStep.DSO_RESTORE_CONNECTIONS;
import static energy.usef.dso.workflow.operate.DsoMonitorGridStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexOrder;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuState;
import energy.usef.core.model.RegimeType;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CoreBusinessError;
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.FlexOfferTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;

/**
 * This coordinater executes the business logic for the DSO operate phase.
 */
@Stateless
public class DsoOperateCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoOperateCoordinator.class);

    private static final int MINUTES_PER_DAY = 24 * 60;

    @Inject
    private Config config;

    @Inject
    private ConfigDso configDso;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    /**
     * This method starts the workflow when triggered by an event.
     *
     * @param event {@link SendOperateEvent} event which starts the workflow.
     */
    @Asynchronous
    public void sendOperate(@Observes SendOperateEvent event) throws BusinessException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        LocalDateTime currentDateTime = DateTimeUtil.getCurrentDateTime();
        int currentPtuIndex = PtuUtil.getPtuIndex(currentDateTime, config.getIntegerProperty(ConfigParam.PTU_DURATION));

        // Map: Congestion point entity address -> Map: PTU Date -> Flex Offer list
        Map<String, Map<LocalDate, List<PlanboardMessage>>> offersPerCongestionPointPerDateMap = dsoPlanboardBusinessService
                .findOrderableFlexOffers();

        // Map: Congestion point entity address -> Map: PTU Date -> GridSafetyAnalysis DTO
        Map<String, Map<LocalDate, GridSafetyAnalysisDto>> gridSafetyAnalysisPerCongestionPointPerDateMap =
                dsoPlanboardBusinessService.createGridSafetyAnalysisRelatedToFlexOffersDtoMap();

        PtuContainer currentPtuContainer = dsoPlanboardBusinessService.findPtuContainer(currentDateTime.toLocalDate(), currentPtuIndex);
        PtuContainer previousPtuContainer = fetchPreviousPtuContainer(currentDateTime, currentPtuIndex);

        if (currentPtuContainer == null) {
            throw new BusinessException(CoreBusinessError.NOT_INITIALIZED_PLANBOARD, currentDateTime);
        }

        for (CongestionPointConnectionGroup congestionPoint : dsoPlanboardBusinessService
                .findActiveCongestionPointConnectionGroup(currentDateTime.toLocalDate())) {

            PtuState ptuState = dsoPlanboardBusinessService.findOrCreatePtuState(currentPtuContainer, congestionPoint);
            Optional<Long> currentLimitedPower = dsoPlanboardBusinessService.findLimitedPower(currentPtuContainer, congestionPoint);
            if (!currentLimitedPower.isPresent() && previousPtuContainer != null) {
                currentLimitedPower = dsoPlanboardBusinessService.findLimitedPower(previousPtuContainer, congestionPoint);
            }
            Long sumOfPower = currentLimitedPower.orElse(0L);
            if (sumOfPower != 0L) {
                ptuState.setRegime(RegimeType.ORANGE);
            }

            WorkflowContext monitorGridResultContext = invokeMonitorGridPbc(currentDateTime, currentPtuIndex, congestionPoint,
                    sumOfPower);
            WorkflowUtil.validateContext(DsoWorkflowStep.DSO_MONITOR_GRID.name(), monitorGridResultContext,
                    DsoMonitorGridStepParameter.OUT.values());
            long actualLoad = (Long) monitorGridResultContext.getValue(DsoMonitorGridStepParameter.OUT.ACTUAL_LOAD.name());
            long maxLoad = (Long) monitorGridResultContext.getValue(DsoMonitorGridStepParameter.OUT.MAX_LOAD.name());
            long minLoad = (Long) monitorGridResultContext.getValue(DsoMonitorGridStepParameter.OUT.MIN_LOAD.name());
            boolean congestion = (Boolean) monitorGridResultContext.getValue(DsoMonitorGridStepParameter.OUT.CONGESTION.name());

            dsoPlanboardBusinessService.setActualPower(currentPtuContainer, actualLoad, congestionPoint);

            if (congestion) {
                Long orderedPower = handleCongestion(offersPerCongestionPointPerDateMap,
                        gridSafetyAnalysisPerCongestionPointPerDateMap, congestionPoint);

                if (isPowerOutsideLoadLimits(actualLoad, orderedPower, maxLoad, minLoad)) {
                    long powerDecrease = invokeLimitedConnectionsPBC(congestionPoint.getUsefIdentifier(),
                            currentPtuContainer.getPtuDate(), currentPtuIndex);
                    dsoPlanboardBusinessService.setLimitedPower(currentPtuContainer, powerDecrease, congestionPoint);
                    ptuState.setRegime(RegimeType.ORANGE);
                    // set next ptu to orange
                    setNextPtuContainerToOrange(currentPtuContainer, congestionPoint);
                } else {
                    ptuState.setRegime(RegimeType.YELLOW);
                }
            } else if (ptuState.getRegime() == RegimeType.ORANGE) {
                // No congestion any more, so connections can be restored

                // Invoking Restore Limited Connections PBC
                invokeRestoreLimitedConnectionsPBC(congestionPoint.getUsefIdentifier(), currentDateTime.toLocalDate(),
                        currentPtuIndex);

                dsoPlanboardBusinessService.setLimitedPower(currentPtuContainer, null, congestionPoint);
                ptuState.setRegime(RegimeType.YELLOW);
            }

        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private boolean isPowerOutsideLoadLimits(long actualLoad, Long orderedPower, long maxLoad, long minLoad) {
        long load = actualLoad + orderedPower;
        return load < minLoad || load > maxLoad;
    }

    private WorkflowContext invokeMonitorGridPbc(LocalDateTime currentDateTime, int currentPtuIndex,
            CongestionPointConnectionGroup congestionPoint, Long sumOfPower) {
        WorkflowContext contextIn = new DefaultWorkflowContext();
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        contextIn.setValue(DsoMonitorGridStepParameter.IN.PTU_DURATION.name(), ptuDuration);
        contextIn.setValue(DsoMonitorGridStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), congestionPoint.getUsefIdentifier());
        contextIn.setValue(DsoMonitorGridStepParameter.IN.LIMITED_POWER.name(), sumOfPower);
        Long numConnections = dsoPlanboardBusinessService.findConnectionCountByUsefIdentifier(congestionPoint.getUsefIdentifier());
        contextIn.setValue(DsoMonitorGridStepParameter.IN.NUM_CONNECTIONS.name(), numConnections);
        contextIn.setValue(DsoMonitorGridStepParameter.IN.PTU_INDEX.name(), currentPtuIndex);
        contextIn.setValue(DsoMonitorGridStepParameter.IN.PERIOD.name(), currentDateTime.toLocalDate());

        WorkflowContext contextOut = workflowStepExecuter.invoke(DSO_MONITOR_GRID.name(), contextIn);

        WorkflowUtil.validateContext(DSO_MONITOR_GRID.name(), contextOut, DsoMonitorGridStepParameter.OUT.values());
        return contextOut;
    }

    private PtuContainer fetchPreviousPtuContainer(LocalDateTime ptuDate, Integer ptuIndex) {
        PtuContainer previousPtuContainer;
        int previousPtuIndex = ptuIndex - 1;
        if (previousPtuIndex < 1) {
            int amountOfPtus = MINUTES_PER_DAY / config.getIntegerProperty(ConfigParam.PTU_DURATION);
            previousPtuContainer = dsoPlanboardBusinessService.findPtuContainer(ptuDate.minusDays(1).toLocalDate(), previousPtuIndex
                    + amountOfPtus);
        } else {
            previousPtuContainer = dsoPlanboardBusinessService.findPtuContainer(ptuDate.toLocalDate(), previousPtuIndex);
        }
        return previousPtuContainer;
    }

    private void setNextPtuContainerToOrange(PtuContainer currentPtuContainer, ConnectionGroup connectionGroup) {
        PtuContainer nextPtuContainer;
        if (currentPtuContainer.getPtuIndex() == MINUTES_PER_DAY / config.getIntegerProperty(ConfigParam.PTU_DURATION)) {
            nextPtuContainer = dsoPlanboardBusinessService.findPtuContainer(currentPtuContainer.getPtuDate().plusDays(1), 1);
        } else {
            nextPtuContainer = dsoPlanboardBusinessService.findPtuContainer(
                    currentPtuContainer.getPtuDate(),
                    currentPtuContainer.getPtuIndex() + 1);
        }
        dsoPlanboardBusinessService.findOrCreatePtuState(nextPtuContainer, connectionGroup).setRegime(RegimeType.ORANGE);
    }

    private long handleCongestion(Map<String, Map<LocalDate, List<PlanboardMessage>>> offersPerCongestionPointPerDateMap,
            Map<String, Map<LocalDate, GridSafetyAnalysisDto>> gridSafetyAnalysisPerCongestionPointPerDateMap,
            CongestionPointConnectionGroup congestionPoint) {

        LocalDateTime ptuDate = DateTimeUtil.getCurrentDateTime();
        int ptuIndex = PtuUtil.getPtuIndex(ptuDate, config.getIntegerProperty(ConfigParam.PTU_DURATION));

        String usefIdentifier = congestionPoint.getUsefIdentifier();

        Map<LocalDate, List<PlanboardMessage>> flexOffersPerDate = offersPerCongestionPointPerDateMap.get(usefIdentifier);
        List<PlanboardMessage> flexOfferMessages = flexOffersPerDate != null ? flexOffersPerDate.get(ptuDate.toLocalDate()) : null;

        Map<LocalDate, GridSafetyAnalysisDto> gridSafetyAnalysisForCongestionPoint = gridSafetyAnalysisPerCongestionPointPerDateMap
                .get(usefIdentifier);
        GridSafetyAnalysisDto gridSafetyAnalysisDto = gridSafetyAnalysisForCongestionPoint != null ?
                gridSafetyAnalysisForCongestionPoint.get(ptuDate.toLocalDate()) : null;

        long orderedPower = 0L;
        if (flexOfferMessages != null && gridSafetyAnalysisDto != null) {
            List<FlexOfferDto> offerDtos = createFlexOffersDtoWithPtus(flexOfferMessages);

            List<FlexOfferDto> acceptedFlexOffers = invokePlaceOperateFlexOrdersPbc(gridSafetyAnalysisDto, offerDtos);

            storeAndSendFlexOrders(offersPerCongestionPointPerDateMap, acceptedFlexOffers);

            orderedPower = sumAcceptedOrderedPower(ptuIndex, offerDtos, acceptedFlexOffers);
        } else {
            LOGGER.debug("No flex offer and/or grid safety analysis for congestion point {} and date {}",
                    congestionPoint.getUsefIdentifier(), ptuDate);
        }

        return orderedPower;
    }

    private long sumAcceptedOrderedPower(int ptuIndex, List<FlexOfferDto> offerDtos, List<FlexOfferDto> acceptedFlexOffers) {
        long orderedPower = 0L;
        for (FlexOfferDto dto : offerDtos) {
            for (FlexOfferDto acceptedFlexOfferDto : acceptedFlexOffers) {
                if (dto.getSequenceNumber().equals(acceptedFlexOfferDto.getSequenceNumber())) {
                    orderedPower += dto.getPtus().get(ptuIndex - 1).getPower().longValue();
                }
            }
        }
        return orderedPower;
    }

    @SuppressWarnings("unchecked")
    private List<FlexOfferDto> invokePlaceOperateFlexOrdersPbc(GridSafetyAnalysisDto gridSafetyAnalysisDto,
            List<FlexOfferDto> offerDtos) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        inContext.setValue(PlaceOperateFlexOrdersStepParameter.IN.PTU_DURATION.name(), ptuDuration);
        inContext.setValue(PlaceOperateFlexOrdersStepParameter.IN.FLEX_OFFER_DTO_LIST.name(), offerDtos);
        inContext.setValue(PlaceOperateFlexOrdersStepParameter.IN.GRID_SAFETY_ANALYSIS_DTO.name(), gridSafetyAnalysisDto);

        // determine which flex offers are accepted. The flex offers which will be accepted are turned into flex
        // order.
        WorkflowContext outContext = workflowStepExecuter.invoke(DSO_PLACE_OPERATE_FLEX_ORDERS.name(), inContext);

        return outContext.get(PlaceOperateFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_DTO_LIST.name(), List.class);
    }

    private long invokeLimitedConnectionsPBC(String usefIdentifier, LocalDate ptuDate, int ptuIndex) {
        WorkflowContext workflowContext = new DefaultWorkflowContext();
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        workflowContext.setValue(DsoLimitConnectionsStepParameter.IN.PTU_DURATION.name(), ptuDuration);
        workflowContext.setValue(DsoLimitConnectionsStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), usefIdentifier);
        workflowContext.setValue(DsoLimitConnectionsStepParameter.IN.PERIOD.name(), ptuDate);
        workflowContext.setValue(DsoLimitConnectionsStepParameter.IN.PTU_INDEX.name(), ptuIndex);

        workflowStepExecuter.invoke(DSO_LIMIT_CONNECTIONS.name(), workflowContext);
        return (long) workflowContext.getValue(DsoLimitConnectionsStepParameter.OUT.POWER_DECREASE.name());
    }

    private void invokeRestoreLimitedConnectionsPBC(String usefIdentifier, LocalDate ptuDate, int ptuIndex) {
        WorkflowContext workflowContext = new DefaultWorkflowContext();
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        workflowContext.setValue(RestoreConnectionsStepParameter.IN.PTU_DURATION.name(), ptuDuration);
        workflowContext.setValue(RestoreConnectionsStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), usefIdentifier);
        workflowContext.setValue(RestoreConnectionsStepParameter.IN.PERIOD.name(), ptuDate);
        workflowContext.setValue(RestoreConnectionsStepParameter.IN.PTU_INDEX.name(), ptuIndex);

        workflowStepExecuter.invoke(DSO_RESTORE_CONNECTIONS.name(), workflowContext);
        // No output is expected from this PBC
    }

    private void storeAndSendFlexOrders(Map<String, Map<LocalDate, List<PlanboardMessage>>> flexOffersPerCongestionPointPerDateMap,
            List<FlexOfferDto> acceptedFlexOfferDtos) {
        for (FlexOfferDto acceptedFlexOfferDto : acceptedFlexOfferDtos) {

            PlanboardMessage acceptedFlexOffer = getOfferBySequence(flexOffersPerCongestionPointPerDateMap, acceptedFlexOfferDto);
            Long flexOrderSequence = sequenceGeneratorService.next();

            // create and send flex order message.
            FlexOrder flexOrderMessage = createFlexOrderMessage(acceptedFlexOffer, acceptedFlexOfferDto, flexOrderSequence,
                    DateTimeUtil.getEndOfDay(acceptedFlexOfferDto.getPeriod()));

            // store flex order on the planboard.
            corePlanboardBusinessService.storeFlexOrder(flexOrderMessage.getCongestionPoint(), flexOrderMessage,
                    DocumentStatus.SENT, flexOrderMessage.getMessageMetadata()
                            .getRecipientDomain(), AcknowledgementStatus.SENT);

            jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(flexOrderMessage));

            // set the offer status on PROCESSED, so it won't be processed again.
            acceptedFlexOffer.setDocumentStatus(DocumentStatus.PROCESSED);
        }
    }

    private FlexOrder createFlexOrderMessage(PlanboardMessage offer, FlexOfferDto offerDto, Long flexOrderSequence,
            LocalDateTime validUntil) {
        FlexOrder flexOrderMessage = new FlexOrder();

        flexOrderMessage.setMessageMetadata(MessageMetadataBuilder.build(offer.getParticipantDomain(), USEFRole.AGR,
                config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.DSO, ROUTINE).validUntil(validUntil).build());
        flexOrderMessage.setCongestionPoint(offer.getConnectionGroup().getUsefIdentifier());
        flexOrderMessage.setCurrency(config.getProperty(ConfigParam.CURRENCY));
        flexOrderMessage.setTimeZone(config.getProperty(ConfigParam.TIME_ZONE));
        flexOrderMessage.setPTUDuration(Period.minutes(config.getIntegerProperty(ConfigParam.PTU_DURATION)));
        flexOrderMessage.setFlexOfferOrigin(offer.getParticipantDomain());
        flexOrderMessage.setFlexOfferSequence(offer.getSequence());
        flexOrderMessage.setSequence(flexOrderSequence);
        flexOrderMessage.setOrderReference(UUID.randomUUID().toString());
        flexOrderMessage.setPeriod(offer.getPeriod());
        flexOrderMessage.setExpirationDateTime(validUntil);

        offerDto.getPtus().stream().map(FlexOfferTransformer::transformToPTU).collect(Collectors.toList());

        List<PTU> ptus = offerDto.getPtus().stream().map(FlexOfferTransformer::transformToPTU).collect(Collectors.toList());
        flexOrderMessage.getPTU().addAll(PtuListConverter.compact(ptus));
        return flexOrderMessage;
    }

    private PlanboardMessage getOfferBySequence(
            Map<String, Map<LocalDate, List<PlanboardMessage>>> flexOffersPerCongestionPointPerDate, FlexOfferDto flexOfferDto) {
        List<PlanboardMessage> flexOffers = flexOffersPerCongestionPointPerDate.get(flexOfferDto.getConnectionGroupEntityAddress())
                .get(flexOfferDto.getPeriod());
        return flexOffers.stream().filter(o -> o.getSequence().equals(flexOfferDto.getSequenceNumber())).findAny().get();
    }

    private List<FlexOfferDto> createFlexOffersDtoWithPtus(List<PlanboardMessage> offers) {
        List<FlexOfferDto> offerDtos = offers.stream().map(FlexOfferTransformer::transform).collect(Collectors.toList());

        for (FlexOfferDto offerDto : offerDtos) {
            offerDto.setPtus(
                    corePlanboardBusinessService.findPtuFlexOffer(offerDto.getSequenceNumber(), offerDto.getParticipantDomain())
                            .values().stream().map(FlexOfferTransformer::transformPtuFlexOffer).collect(Collectors.toList()));
        }
        return offerDtos;
    }

}
