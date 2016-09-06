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

package energy.usef.brp.workflow.plan.aplan.missing;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.brp.service.business.BrpPlanboardBusinessService;
import energy.usef.brp.workflow.BrpWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.data.xml.bean.message.PrognosisType;
import energy.usef.core.event.DayAheadClosureEvent;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.AgrConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
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

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create Missing A-Plans workflow for BRP.
 */
@Stateless
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class BrpCreateMissingAPlansCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrpCreateMissingAPlansCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private BrpPlanboardBusinessService brpPlanboardBusinessService;

    @Inject
    private Config config;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * This method handles the DayAheadClosureEvent. Missing A-Plans can be created.
     *
     * @param event the {@link DayAheadClosureEvent} that triggers the process.
     */
    public void handleEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) DayAheadClosureEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodInFuture(event);

        LocalDate period = event.getPeriod();

        Map<ConnectionGroup, List<Connection>> activeConnectionGroupsWithConnections = corePlanboardBusinessService
                .findActiveConnectionGroupsWithConnections(period);

        Map<ConnectionGroup, List<PtuPrognosis>> ptuPrognosisPerConnectionGroup = corePlanboardBusinessService
                .findLastPrognoses(period, energy.usef.core.model.PrognosisType.A_PLAN).stream()
                .collect(Collectors.groupingBy(PtuPrognosis::getConnectionGroup));

        activeConnectionGroupsWithConnections.entrySet().stream()
                .filter(connectionGroupListEntry -> connectionGroupListEntry.getKey() instanceof AgrConnectionGroup)
                .forEach(connectionGroupListEntry -> {
                    AgrConnectionGroup agrConnectionGroup = (AgrConnectionGroup) connectionGroupListEntry.getKey();
                    List<Connection> connectionList = connectionGroupListEntry.getValue();
                    List<PtuPrognosis> latestAPlans = ptuPrognosisPerConnectionGroup.get(agrConnectionGroup);

                    if (latestAPlans == null || latestAPlans.isEmpty()) {
                        PrognosisDto prognosisDto = invokeCreateMissingAPlansPBC(agrConnectionGroup.getAggregatorDomain(), period,
                                connectionList);

                        storeAPlans(prognosisDto, agrConnectionGroup);
                    }
                });

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void storeAPlans(PrognosisDto prognosisDto, AgrConnectionGroup connectionGroup) {
        Prognosis prognosis = new Prognosis();
        prognosis.setPeriod(prognosisDto.getPeriod());
        prognosis.setSequence(sequenceGeneratorService.next() * -1);
        prognosis.setType(PrognosisType.A_PLAN);

        for (PtuPrognosisDto ptuPrognosisDto : prognosisDto.getPtus()) {
            PTU ptu = new PTU();
            ptu.setStart(ptuPrognosisDto.getPtuIndex());
            ptu.setPower(ptuPrognosisDto.getPower());
            ptu.setDuration(BigInteger.ONE);
            prognosis.getPTU().add(ptu);
        }

        corePlanboardBusinessService.storePrognosis(prognosis, connectionGroup, DocumentType.A_PLAN, DocumentStatus.ACCEPTED,
                connectionGroup.getAggregatorDomain(),null,  true);
    }

    private PrognosisDto invokeCreateMissingAPlansPBC(String aggregatorDomain, LocalDate period, List<Connection> connectionList) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(BrpCreateMissingAPlansParamater.IN.AGGREGATOR_DOMAIN.name(), aggregatorDomain);
        inContext.setValue(BrpCreateMissingAPlansParamater.IN.PERIOD.name(), period);
        inContext.setValue(BrpCreateMissingAPlansParamater.IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        inContext.setValue(BrpCreateMissingAPlansParamater.IN.CONNECTION_COUNT.name(), connectionList.size());

        WorkflowContext outContext = workflowStepExecuter.invoke(BrpWorkflowStep.BRP_CREATE_MISSING_A_PLANS.name(), inContext);

        WorkflowUtil.validateContext(BrpWorkflowStep.BRP_CREATE_MISSING_A_PLANS.name(), outContext, BrpCreateMissingAPlansParamater.OUT.values());

        PrognosisDto prognosisDto = outContext.get(BrpCreateMissingAPlansParamater.OUT.PROGNOSIS_DTO.name(), PrognosisDto.class);
        prognosisDto.setSubstitute(true);

        return prognosisDto;
    }

}
