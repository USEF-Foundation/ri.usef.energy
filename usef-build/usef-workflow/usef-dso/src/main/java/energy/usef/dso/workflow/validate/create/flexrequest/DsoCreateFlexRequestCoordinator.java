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

package energy.usef.dso.workflow.validate.create.flexrequest;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;

import java.util.Comparator;
import java.util.List;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexRequest;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.Document;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.FlexRequestTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.model.GridSafetyAnalysis;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.transformer.GridSafetyAnalysisDtoTransformer;

/**
 * Coordinator class for the 'Create Flex Request' workflow.
 * <p/>
 * Stateless, because this process can and should be executed simultaneous / aggregator
 */
@Stateless
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class DsoCreateFlexRequestCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoCreateFlexRequestCoordinator.class);

    @Inject
    private Config config;

    @Inject
    private ConfigDso configDso;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Inject
    private WorkflowStepExecuter workflowStubLoader;

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * {@inheritDoc}
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void createFlexRequests(@Observes(during = TransactionPhase.AFTER_COMPLETION) CreateFlexRequestEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);

        // 1. Invoke step 2004: transforms the grid safety analysis of one day into flex requests
        List<GridSafetyAnalysis> gridSafetyAnalysis = dsoPlanboardBusinessService
                .findLatestGridSafetyAnalysisWithDispositionRequested(
                        event.getCongestionPointEntityAddress(), event.getPeriod());
        if (gridSafetyAnalysis.isEmpty()) {
            LOGGER.info(
                    "No grid analysis with requested disposition has been found for congestion point {} on {}. Workflow will end.",
                    event.getCongestionPointEntityAddress(), event.getPeriod());
            return;
        }
        List<FlexRequestDto> flexRequestDtos = invokeCreateFlexRequestPbc(event, gridSafetyAnalysis);

        if (flexRequestDtos == null || flexRequestDtos.isEmpty()) {
            LOGGER.info("No flex requests to send. Workflow will end.");
            return;
        }

        // 2.3 For each aggregator active on the congestion point
        List<Aggregator> aggregators = dsoPlanboardBusinessService.getAggregatorsByCongestionPointAddress(
                event.getCongestionPointEntityAddress(), event.getPeriod());
        // 2.1 Initialize a new flex request
        createAndSendFlexRequests(event, gridSafetyAnalysis, flexRequestDtos, aggregators);
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void createAndSendFlexRequests(CreateFlexRequestEvent event, List<GridSafetyAnalysis> gridSafetyAnalysis,
            List<FlexRequestDto> flexRequestDtos, List<Aggregator> aggregators) {
        for (FlexRequestDto flexRequestDto : flexRequestDtos) {
            FlexRequest flexRequestMessage = initializeFlexRequestMessage(flexRequestDto, event);
            for (Aggregator aggregator : aggregators) {
                flexRequestMessage.getMessageMetadata().setRecipientDomain(aggregator.getDomain());
                // ensure different UUIDs for each aggregator.
                flexRequestMessage.getMessageMetadata().setConversationID(MessageMetadataBuilder.uuid());
                flexRequestMessage.getMessageMetadata().setMessageID(MessageMetadataBuilder.uuid());

                Long prognosisSequence = findRelatedPrognosisSequenceForFlexRequest(gridSafetyAnalysis, aggregator.getDomain());
                if (prognosisSequence == null) {
                    LOGGER.warn("No prognosis has been found for the aggregator {}. The flex request will not be sent.",
                            aggregator.getDomain());
                    continue;
                }
                // assign the domain
                flexRequestMessage.setPrognosisOrigin(aggregator.getDomain());
                flexRequestMessage.setPrognosisSequence(prognosisSequence);

                corePlanboardBusinessService.storeFlexRequest(flexRequestMessage.getCongestionPoint(), flexRequestMessage,
                        DocumentStatus.SENT, aggregator.getDomain());

                // 2.3.1 Send the flex request message
                jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(flexRequestMessage));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<FlexRequestDto> invokeCreateFlexRequestPbc(CreateFlexRequestEvent event,
            List<GridSafetyAnalysis> gridSafetyAnalysis) {

        WorkflowContext contextIn = new DefaultWorkflowContext();
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        contextIn.setValue(CreateFlexRequestStepParameter.IN.PTU_DURATION.name(), ptuDuration);
        contextIn.setValue(CreateFlexRequestStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(),
                event.getCongestionPointEntityAddress());
        contextIn.setValue(CreateFlexRequestStepParameter.IN.PERIOD.name(), event.getPeriod());

        GridSafetyAnalysisDto gridSafetyAnalysisDto = GridSafetyAnalysisDtoTransformer.transform(gridSafetyAnalysis);
        contextIn.setValue(CreateFlexRequestStepParameter.IN.GRID_SAFETY_ANALYSIS_DTO.name(), gridSafetyAnalysisDto);

        WorkflowContext contextOut = workflowStubLoader.invoke(DsoWorkflowStep.DSO_CREATE_FLEX_REQUEST.name(), contextIn);

        WorkflowUtil.validateContext(DsoWorkflowStep.DSO_CREATE_FLEX_REQUEST.name(), contextOut,
                CreateFlexRequestStepParameter.OUT.values());

        return contextOut.get(CreateFlexRequestStepParameter.OUT.FLEX_REQUESTS_DTO_LIST.name(), List.class);
    }

    private Long findRelatedPrognosisSequenceForFlexRequest(List<GridSafetyAnalysis> gridSafetyAnalysis,
            String aggregatorDomain) {
        return gridSafetyAnalysis.stream()
                .flatMap(ptuGridSafetyAnalysis -> ptuGridSafetyAnalysis.getPrognoses()
                        .stream()
                        .filter(prognosis -> prognosis.getParticipantDomain().equals(aggregatorDomain)))
                .map(Document::getSequence)
                .max(Comparator.<Long>naturalOrder()).orElse(null);
    }

    private FlexRequest initializeFlexRequestMessage(FlexRequestDto flexRequestDto, CreateFlexRequestEvent event) {
        // Sets the PTU
        FlexRequest flexRequestMessage = new FlexRequest();
        flexRequestMessage.getPTU().addAll(FlexRequestTransformer.transformPtusToXml(flexRequestDto.getPtus()));
        // Sets the metadata
        MessageMetadataBuilder metadataBuilder = new MessageMetadataBuilder();
        metadataBuilder.timeStamp()
                .precedence(TRANSACTIONAL)
                .senderRole(USEFRole.DSO)
                .senderDomain(config.getProperty(ConfigParam.HOST_DOMAIN))
                .validUntil(flexRequestDto.getExpirationDateTime())
                .recipientRole(USEFRole.AGR);
        flexRequestMessage.setMessageMetadata(metadataBuilder.build());
        // Sets remaining fields for the flexRequest
        flexRequestMessage.setCongestionPoint(event.getCongestionPointEntityAddress());
        flexRequestMessage.setExpirationDateTime(flexRequestDto.getExpirationDateTime());
        flexRequestMessage.setPTUDuration(Period.minutes(config.getIntegerProperty(ConfigParam.PTU_DURATION)));
        flexRequestMessage.setPeriod(event.getPeriod());
        flexRequestMessage.setTimeZone(config.getProperty(ConfigParam.TIME_ZONE));
        flexRequestMessage.setSequence(sequenceGeneratorService.next());
        return flexRequestMessage;
    }
}
