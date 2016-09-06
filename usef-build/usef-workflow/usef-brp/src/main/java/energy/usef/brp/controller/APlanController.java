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

package energy.usef.brp.controller;

import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.brp.service.business.BrpPlanboardValidatorService;
import energy.usef.brp.workflow.plan.connection.forecast.ReceivedAPlanEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.controller.BaseIncomingMessageController;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.data.xml.bean.message.PrognosisResponse;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.XMLUtil;

import java.util.List;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes initial A-plan reception.
 */
@Stateless
public class APlanController extends BaseIncomingMessageController<Prognosis> {
    private static final Logger LOGGER = LoggerFactory.getLogger(APlanController.class);

    @Inject
    private CorePlanboardValidatorService corePlanboardValidatorService;

    @Inject
    private BrpPlanboardValidatorService brpPlanboardValidatorService;

    @Inject
    private Event<ReceivedAPlanEvent> receivedAPlanEventManager;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private Config config;

    /**
     * {@inheritDoc}
     */
    public void action(Prognosis aPlan, Message savedMessage) throws BusinessException {
        LOGGER.info("A-Plan received for date {} with {} PTUs", aPlan.getPeriod(), aPlan.getPTU().size());

        String usefIdentifier = aPlan.getMessageMetadata().getSenderDomain();

        // Normalizing PTU list.
        List<PTU> normalizedPtus = PtuListConverter.normalize(aPlan.getPTU());
        aPlan.getPTU().clear();
        aPlan.getPTU().addAll(normalizedPtus);

        try {
            corePlanboardValidatorService.validatePTUDuration(aPlan.getPTUDuration());
            corePlanboardValidatorService.validateTimezone(aPlan.getTimeZone());
            brpPlanboardValidatorService.validatePlanboardHasBeenInitialized(aPlan.getPeriod(), aPlan.getPTU());
            brpPlanboardValidatorService.validatePtus(aPlan.getPTU());
            brpPlanboardValidatorService.validatePeriod(aPlan.getPeriod());
            brpPlanboardValidatorService.validateAPlanSequenceNumber(aPlan);
            brpPlanboardValidatorService.validateAPlanConnectionGroup(aPlan.getPeriod(), usefIdentifier);
        } catch (BusinessValidationException e) {
            LOGGER.warn( e.getMessage(), e);
            sendRejectedPrognosisResponse(aPlan, e.getMessage());
            return;
        }

        // Archive previously received A-Plans first.
        corePlanboardBusinessService.archiveAPlans(usefIdentifier, aPlan.getPeriod());

        // Store the received A-Plan in the Planboard. No prognosis response is sent at this moment.
        corePlanboardBusinessService.storePrognosis(usefIdentifier, aPlan, DocumentType.A_PLAN, DocumentStatus.RECEIVED,
                usefIdentifier, savedMessage, false);
        receivedAPlanEventManager.fire(new ReceivedAPlanEvent(aPlan.getPeriod()));
    }

    private void sendRejectedPrognosisResponse(Prognosis prognosis, String errorMessage) {
        PrognosisResponse prognosisResponse = createRejectedPrognosisResponse(prognosis.getMessageMetadata().getConversationID(),
                prognosis.getMessageMetadata().getSenderDomain(), prognosis.getSequence(), errorMessage);
        putMessageIntoOutgoingQueue(prognosisResponse);
    }

    private PrognosisResponse createRejectedPrognosisResponse(String conversationID, String recipientDomain,
            Long prognosisSequence,
            String errorMessage) {
        PrognosisResponse prognosisResponse = new PrognosisResponse();

        MessageMetadata messageMetadata = new MessageMetadataBuilder().precedence(ROUTINE)
                .messageID()
                .timeStamp()
                .conversationID(conversationID)
                .senderDomain(config.getProperty(ConfigParam.HOST_DOMAIN))
                .senderRole(USEFRole.BRP)
                .recipientDomain(recipientDomain)
                .recipientRole(USEFRole.AGR)
                .build();

        prognosisResponse.setPrognosisSequence(prognosisSequence);

        // Errors
        LOGGER.info("A-Plan rejected");
        prognosisResponse.setResult(DispositionAcceptedRejected.REJECTED);
        prognosisResponse.setMessage(errorMessage);

        prognosisResponse.setMessageMetadata(messageMetadata);

        return prognosisResponse;
    }

    private void putMessageIntoOutgoingQueue(PrognosisResponse xmlObject) {
        String xml = XMLUtil.messageObjectToXml(xmlObject);
        jmsHelperService.sendMessageToOutQueue(xml);
    }

}
