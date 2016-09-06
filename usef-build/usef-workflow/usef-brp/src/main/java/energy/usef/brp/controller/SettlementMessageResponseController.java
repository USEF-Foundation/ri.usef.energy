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

import energy.usef.brp.workflow.settlement.send.BrpSettlementMessageResponseCoordinator;
import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedDisputed;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOrderSettlement;
import energy.usef.core.data.xml.bean.message.FlexOrderSettlementStatus;
import energy.usef.core.data.xml.bean.message.SettlementMessage;
import energy.usef.core.data.xml.bean.message.SettlementMessageResponse;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;
import energy.usef.core.util.XMLUtil;

import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message controller class handling the processing a {@link SettlementMessageResponse}.
 */
@Stateless
public class SettlementMessageResponseController extends BaseIncomingResponseMessageController<SettlementMessageResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementMessageResponseController.class);

    @Inject
    private BrpSettlementMessageResponseCoordinator coordinator;

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(SettlementMessageResponse message, Message savedMessage) throws BusinessException {

        LOGGER.info("Received SettlementMessageResponse whose result is {}.", message.getResult());
        String aggregatorDomain = message.getMessageMetadata().getSenderDomain();
        if (DispositionAcceptedRejected.ACCEPTED == message.getResult()) {
            // mark all flex orders settlement as accepted or disputed
            coordinator.processPtuSettlements(fetchFlexOrderSettlementSequencesWithDisposition(
                    message.getFlexOrderSettlementStatus(),
                    DispositionAcceptedDisputed.ACCEPTED), DispositionAcceptedDisputed.ACCEPTED, aggregatorDomain);
            coordinator.processPtuSettlements(fetchFlexOrderSettlementSequencesWithDisposition(
                    message.getFlexOrderSettlementStatus(),
                    DispositionAcceptedDisputed.DISPUTED), DispositionAcceptedDisputed.DISPUTED, aggregatorDomain);
        } else {
            // mark all flex orders settlement as disputed
            List<Long> references = fetchOrigninalFlexOrderSettlementSequences(message.getMessageMetadata().getConversationID());
            coordinator.processPtuSettlements(references, DispositionAcceptedDisputed.DISPUTED, aggregatorDomain);
        }
    }

    /**
     * Fetch the list of flex order sequences related to the {@link FlexOrderSettlementStatus} with the specified disposition in the
     * given list of {@link FlexOrderSettlementStatus}.
     * 
     * @param statuses {@link List} of {@link FlexOrderSettlementStatus}.
     * @param disposition {@link DispositionAcceptedDisputed} of the {@link FlexOrderSettlementStatus}.
     * @return {@link List} of @ Long} , the Flex order sequences.
     */
    private List<Long> fetchFlexOrderSettlementSequencesWithDisposition(List<FlexOrderSettlementStatus> statuses,
            DispositionAcceptedDisputed disposition) {
        return statuses.stream()
                .filter(status -> disposition == DispositionAcceptedDisputed.valueOf(status.getDisposition().name()))
                .map(status -> Long.valueOf(status.getOrderReference()))
                .collect(Collectors.toList());
    }

    /**
     * Fetch the list of flex order sequences related to the {@link FlexOrderSettlement} sent in the initial
     * {@link SettlementMessage} having the specified conversation ID.
     * 
     * @param conversationID {@link String} Conversation ID of the initial message.
     * @return {@link List} of @ Long} , the Flex order sequences.
     */
    private List<Long> fetchOrigninalFlexOrderSettlementSequences(String conversationID) {
        String settlementMessageXml = messageService.getMessageResponseByConversationId(conversationID).getXml();
        SettlementMessage settlementMessage = XMLUtil.xmlToMessage(settlementMessageXml, SettlementMessage.class);
        return settlementMessage.getFlexOrderSettlement().stream()
                .map(flexOrderSettlement -> Long.valueOf(flexOrderSettlement.getOrderReference()))
                .collect(Collectors.toList());
    }
}
