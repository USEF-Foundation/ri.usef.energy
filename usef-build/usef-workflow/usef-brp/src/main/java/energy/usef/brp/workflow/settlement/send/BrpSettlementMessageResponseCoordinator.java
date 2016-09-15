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

package energy.usef.brp.workflow.settlement.send;

import energy.usef.core.data.xml.bean.message.DispositionAcceptedDisputed;
import energy.usef.core.data.xml.bean.message.SettlementMessageResponse;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DocumentStatusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.ejb.Singleton;
import javax.inject.Inject;

/**
 * Brp Coordinator in charge of the workflow related to the reception of a {@link SettlementMessageResponse} message.
 */
@Singleton
public class BrpSettlementMessageResponseCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrpSettlementMessageResponseCoordinator.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    /**
     * Process the PTUs settlements related to the specified orders.
     *
     * @param orderReferences  {@link List} of order references which have to be processed.
     * @param disposition      {@link DispositionAcceptedDisputed} which has to be set.
     * @param aggregatorDomain {@link String} domain name of the corresponding aggregator.
     */
    public void processPtuSettlements(List<Long> orderReferences, DispositionAcceptedDisputed disposition,
                                      String aggregatorDomain) {
        for (Long orderReference : orderReferences) {
            List<PlanboardMessage> flexOrderSettlementMessages = corePlanboardBusinessService
                    .findPlanboardMessagesWithOriginSequence(orderReference, DocumentType.FLEX_ORDER_SETTLEMENT, aggregatorDomain);
            for (PlanboardMessage flexOrderSettlementMessage : flexOrderSettlementMessages) {
                if (!DocumentStatus.SENT.equals(flexOrderSettlementMessage.getDocumentStatus())) {
                    LOGGER.error("A response has already been processed for this settlement message %s. Invalid " +
                            "response received from %s. ", flexOrderSettlementMessage.getSequence(), aggregatorDomain);
                    continue;
                }
                flexOrderSettlementMessage.setDocumentStatus(DocumentStatusUtil.toDocumentStatus(disposition));
                corePlanboardBusinessService.updatePlanboardMessage(flexOrderSettlementMessage);
            }
        }
    }
}
