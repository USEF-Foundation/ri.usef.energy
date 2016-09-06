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

package energy.usef.brp.workflow.plan.flexrequest.create;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexRequest;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.transformer.DispositionTransformer;

import java.math.BigInteger;
import java.util.List;

import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class for the workflow describing the creation and sending of flex requests from the BRP to the AGR.
 */
@Singleton
public class BrpCreateFlexRequestCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrpCreateFlexRequestCoordinator.class);

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private Config config;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * {@inheritDoc}
     */
    public void handleEvent(@Observes CreateFlexRequestEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        List<FlexRequestDto> flexRequestDtos = event.getFlexRequestDtos();
        for (FlexRequestDto flexRequestDto : flexRequestDtos) {
            FlexRequest xmlFlexRequest = completeFlexRequestMessage(flexRequestDto);

            // need to link the flex request to a connection group.
            String usefIdentifier = xmlFlexRequest.getMessageMetadata().getRecipientDomain();
            corePlanboardBusinessService.storeFlexRequest(usefIdentifier, xmlFlexRequest, DocumentStatus.SENT, usefIdentifier);
            sendFlexRequestMessage(xmlFlexRequest);
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void sendFlexRequestMessage(FlexRequest flexRequest) {
        jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(flexRequest));
    }

    /**
     * Builds a flex request message with the given parameters.
     *
     * @param flexRequestDto {@link FlexRequestDto} with the basic information.
     * @return a {@link FlexRequest} message.
     */
    private FlexRequest completeFlexRequestMessage(FlexRequestDto flexRequestDto) {
        FlexRequest flexRequest = new FlexRequest();
        flexRequest.setPrognosisOrigin(flexRequestDto.getParticipantDomain());
        flexRequest.setPrognosisSequence(flexRequestDto.getPrognosisSequenceNumber());
        flexRequest.setSequence(sequenceGeneratorService.next());
        flexRequest.setPeriod(flexRequestDto.getPeriod());
        flexRequest.setPTUDuration(Period.minutes(config.getIntegerProperty(ConfigParam.PTU_DURATION)));
        flexRequest.setTimeZone(config.getProperty(ConfigParam.TIME_ZONE));
        flexRequest.setMessageMetadata(buildMessageMetadata(flexRequestDto.getParticipantDomain()));
        flexRequest.setExpirationDateTime(flexRequestDto.getExpirationDateTime());
        flexRequestDto.getPtus().stream().forEach(ptuFlexRequest -> {
            PTU ptu = new PTU();
            ptu.setStart(ptuFlexRequest.getPtuIndex());
            ptu.setDuration(BigInteger.ONE);
            ptu.setDisposition(DispositionTransformer.transformToXml(ptuFlexRequest.getDisposition()));
            ptu.setPower(ptuFlexRequest.getPower());
            flexRequest.getPTU().add(ptu);
        });
        return flexRequest;
    }

    private MessageMetadata buildMessageMetadata(String aggregatorDomain) {
        return new MessageMetadataBuilder().messageID()
                .conversationID()
                .timeStamp()
                .precedence(TRANSACTIONAL)
                .recipientDomain(aggregatorDomain)
                .recipientRole(USEFRole.AGR)
                .senderDomain(config.getProperty(ConfigParam.HOST_DOMAIN))
                .senderRole(USEFRole.BRP)
                .build();
    }
}
