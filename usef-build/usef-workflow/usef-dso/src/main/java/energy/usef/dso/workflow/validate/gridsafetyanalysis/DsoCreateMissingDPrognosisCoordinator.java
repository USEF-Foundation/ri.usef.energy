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

package energy.usef.dso.workflow.validate.gridsafetyanalysis;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.data.xml.bean.message.PrognosisType;
import energy.usef.core.event.DayAheadClosureEvent;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;
import energy.usef.dso.model.AggregatorOnConnectionGroupState;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;

/**
 * Grid Safety Analysis workflow coordinator.
 * <p>
 * This is not a singleton, because this process should be able to run per congestionPoint.
 */
@Stateless
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class DsoCreateMissingDPrognosisCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoCreateMissingDPrognosisCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Inject
    private Config config;

    @Inject
    private Event<GridSafetyAnalysisEvent> gridSafetyEventManager;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * This method handles the DayAheadClosureEvent. Missing D-Prognosis can be created. Additionally GridSafetyAnalysis will be
     * triggered when needed.
     *
     * @param event The {@link DayAheadClosureEvent} that triggers the process.
     */
    public void handleEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) DayAheadClosureEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodInFuture(event);

        LocalDate analysisDay = event.getPeriod();

        // fetch number of agr per congestion point
        Map<CongestionPointConnectionGroup, List<AggregatorOnConnectionGroupState>> connectionGroupsWithAggregators =
                dsoPlanboardBusinessService.findConnectionGroupsWithAggregators(analysisDay);

        corePlanboardBusinessService
                .findActiveCongestionPointAddresses(analysisDay)
                .stream()
                .filter(congestionPointEntityAddress -> connectionGroupsWithAggregators
                        .containsKey(new CongestionPointConnectionGroup(congestionPointEntityAddress)))
                .forEach(congestionPointEntityAddress -> {
                    List<AggregatorOnConnectionGroupState> aggregatorList = connectionGroupsWithAggregators.get(
                            new CongestionPointConnectionGroup(congestionPointEntityAddress));
                    LOGGER.debug("Found [{}] aggregators ", aggregatorList.size());

                    Map<String, List<PtuPrognosis>> prognosisListMap = createDPrognosisPerAggregatorMap(congestionPointEntityAddress,
                            analysisDay);

                    boolean created = createAndAddMissingDPrognosesIfNeeded(aggregatorList, prognosisListMap,
                            congestionPointEntityAddress, analysisDay);

                    if (created || aggregatorList.isEmpty()) {
                        gridSafetyEventManager.fire(new GridSafetyAnalysisEvent(congestionPointEntityAddress, analysisDay));
                    }
                });

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private boolean createAndAddMissingDPrognosesIfNeeded(List<AggregatorOnConnectionGroupState> aggregatorList,
            Map<String, List<PtuPrognosis>> prognosisMap, String usefIdentifier, LocalDate analysisDay) {
        boolean createdPrognosis = false;
        for (AggregatorOnConnectionGroupState aggregatorOnConnectionGroupState : aggregatorList) {
            if (!prognosisMap.containsKey(aggregatorOnConnectionGroupState.getAggregator().getDomain())) {
                LOGGER.debug("There is no D-Prognosis for the aggregator '{}', the prognosis will be created",
                        aggregatorOnConnectionGroupState.getAggregator().getDomain());

                // Invoking PBC
                PrognosisDto prognosisDto = invokeMissingPrognosisPBC(aggregatorOnConnectionGroupState, usefIdentifier,
                        analysisDay);

                // Storing D-Prognosis
                storeDPrognosis(prognosisDto, aggregatorOnConnectionGroupState.getAggregator().getDomain(), usefIdentifier,
                        analysisDay);
                createdPrognosis = true;
            }
        }
        return createdPrognosis;
    }

    private PrognosisDto invokeMissingPrognosisPBC(AggregatorOnConnectionGroupState aggregatorOnConnectionGroupState,
            String usefIdentifier, LocalDate analysisDay) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(CreateMissingDPrognosisParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), usefIdentifier);
        inContext.setValue(
                CreateMissingDPrognosisParameter.IN.AGGREGATOR_DOMAIN.name(), aggregatorOnConnectionGroupState.getAggregator().getDomain());
        inContext.setValue(CreateMissingDPrognosisParameter.IN.ANALYSIS_DAY.name(), analysisDay);
        inContext.setValue(CreateMissingDPrognosisParameter.IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        inContext
                .setValue(CreateMissingDPrognosisParameter.IN.AGGREGATOR_CONNECTION_AMOUNT.name(), aggregatorOnConnectionGroupState.getConnectionCount().intValue());

        WorkflowContext outContext = workflowStepExecuter.invoke(DsoWorkflowStep.DSO_CREATE_MISSING_DPROGNOSES.name(), inContext);
        WorkflowUtil.validateContext(DsoWorkflowStep.DSO_CREATE_MISSING_DPROGNOSES.name(), outContext,
                CreateMissingDPrognosisParameter.OUT.values());

        PrognosisDto prognosisDto = outContext.get(CreateMissingDPrognosisParameter.OUT.D_PROGNOSIS.name(), PrognosisDto.class);
        prognosisDto.setSubstitute(true);

        return prognosisDto;
    }

    private Map<String, List<PtuPrognosis>> createDPrognosisPerAggregatorMap(String congestionPoint, LocalDate analysisDay) {
        Map<String, List<PtuPrognosis>> results = corePlanboardBusinessService
                .findLastPrognoses(analysisDay, energy.usef.core.model.PrognosisType.D_PROGNOSIS, congestionPoint)
                .stream()
                .collect(Collectors.groupingBy(PtuPrognosis::getParticipantDomain));
        LOGGER.debug("Got D-Prognosis: {}", results.entrySet()
                .stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue().size())
                .collect(Collectors.joining()));
        return results;
    }

    private List<PtuPrognosis> storeDPrognosis(PrognosisDto prognosisDto, String participantDomain,
            String congestionPointEntityAddress, LocalDate analysisDay) {
        Prognosis prognosisMessage = new Prognosis();
        prognosisMessage.setPeriod(analysisDay);
        prognosisMessage.setSequence(sequenceGeneratorService.next() * -1);
        prognosisMessage.setType(PrognosisType.D_PROGNOSIS);

        for (PtuPrognosisDto ptuPrognosisDto : prognosisDto.getPtus()) {
            PTU ptu = new PTU();
            ptu.setStart(ptuPrognosisDto.getPtuIndex());
            ptu.setPower(ptuPrognosisDto.getPower());
            prognosisMessage.getPTU().add(ptu);
        }

        return corePlanboardBusinessService.storePrognosis(congestionPointEntityAddress, prognosisMessage,
                DocumentType.D_PROGNOSIS, DocumentStatus.ACCEPTED, participantDomain,null,  true);
    }

}
