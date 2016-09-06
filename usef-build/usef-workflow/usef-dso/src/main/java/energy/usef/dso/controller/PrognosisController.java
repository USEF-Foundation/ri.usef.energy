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

package energy.usef.dso.controller;

import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.controller.BaseIncomingMessageController;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.data.xml.bean.message.PrognosisResponse;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.Message;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.XMLUtil;
import energy.usef.dso.service.business.DsoPlanboardValidatorService;
import energy.usef.dso.workflow.plan.connection.forecast.DsoDPrognosisCoordinator;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes initial d-prognosis reception.
 */
@Stateless
public class PrognosisController extends BaseIncomingMessageController<Prognosis> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrognosisController.class);

    @Inject
    private CorePlanboardValidatorService corePlanboardValidatorService;

    @Inject
    private DsoPlanboardValidatorService dsoPlanboardValidatorService;

    @Inject
    private DsoDPrognosisCoordinator coordinator;

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private Config config;

    /**
     * {@inheritDoc}
     */
    public void action(Prognosis prognosis, Message savedMessage) throws BusinessException {
        LOGGER.info("Prognosis received");
        String entityAddress = prognosis.getCongestionPoint();
        String errorMessage = null;

        try {
            corePlanboardValidatorService.validatePTUDuration(prognosis.getPTUDuration());
            corePlanboardValidatorService.validateTimezone(prognosis.getTimeZone());
            dsoPlanboardValidatorService.validatePtus(prognosis.getPTU());
            dsoPlanboardValidatorService.validateCongestionPoint(entityAddress);
            dsoPlanboardValidatorService.validateAggregator(prognosis.getMessageMetadata().getSenderDomain(),
                    entityAddress, prognosis.getPeriod());
            dsoPlanboardValidatorService.validatePeriod(prognosis.getPeriod());
            dsoPlanboardValidatorService.validatePrognosisSequenceNumber(prognosis);
        } catch (BusinessValidationException e) {
            errorMessage = e.getBusinessError().getError();
            LOGGER.debug(errorMessage, e);
        }

        sendPrognosisResponse(prognosis, errorMessage);
        if (errorMessage != null) {
            return;
        }

        coordinator.invokeWorkflow(prognosis, savedMessage);

    }

    /**
     * Sends the prognosis response XML to AGR.
     *
     * @param prognosis
     * @param errorMessage
     */
    public void sendPrognosisResponse(Prognosis prognosis, String errorMessage) {
        PrognosisResponse prognosisResponse = createPrognosisResponse(prognosis.getCongestionPoint(), prognosis
                .getMessageMetadata().getConversationID(), prognosis.getMessageMetadata().getSenderDomain(), errorMessage);
        prognosisResponse.setPrognosisSequence(prognosis.getSequence());
        putMessageIntoOutgoingQueue(prognosisResponse);
    }

    private PrognosisResponse createPrognosisResponse(String congestionPoint, String conversationID, String recipientDomain,
            String errorMessage) {
        PrognosisResponse prognosisResponse = new PrognosisResponse();

        // Setting root attributes
        prognosisResponse.setMessage(congestionPoint);

        MessageMetadata messageMetadata = new MessageMetadataBuilder()
                .precedence(ROUTINE)
                .messageID()
                .timeStamp()
                .conversationID(conversationID)
                .senderDomain(config.getProperty(ConfigParam.HOST_DOMAIN))
                .senderRole(USEFRole.DSO)
                .recipientDomain(recipientDomain)
                .recipientRole(USEFRole.AGR).build();

        LOGGER.info("Prognosis for {} stored", congestionPoint);

        if (errorMessage == null) {
            // No error
            prognosisResponse.setResult(DispositionAcceptedRejected.ACCEPTED);
            LOGGER.info("No error found, accepted PrognosisResponse will be sent");
        } else {
            // Errors
            LOGGER.info("Prognosis for {} rejected", congestionPoint);
            prognosisResponse.setResult(DispositionAcceptedRejected.REJECTED);
            prognosisResponse.setMessage(errorMessage);
        }

        prognosisResponse.setMessageMetadata(messageMetadata);

        return prognosisResponse;
    }

    private void putMessageIntoOutgoingQueue(PrognosisResponse xmlObject) {
        String xml = XMLUtil.messageObjectToXml(xmlObject);
        jmsHelperService.sendMessageToOutQueue(xml);
    }

}
