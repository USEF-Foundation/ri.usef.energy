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

import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.data.xml.bean.message.MeterDataQueryResponse;
import energy.usef.core.data.xml.bean.message.MeterDataQueryType;
import energy.usef.core.data.xml.bean.message.MeterDataSet;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.dso.workflow.settlement.collect.FinalizeCollectOrangeRegimeDataEvent;
import energy.usef.dso.workflow.settlement.initiate.FinalizeInitiateSettlementEvent;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller class handling the reception of a {@link MeterDataQueryResponse} message.
 */
@Stateless
public class MeterDataQueryResponseController extends BaseIncomingResponseMessageController<MeterDataQueryResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterDataQueryResponseController.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Inject
    private Event<FinalizeInitiateSettlementEvent> finalizeInitiateSettlementEventManager;
    @Inject
    private Event<FinalizeCollectOrangeRegimeDataEvent> finalizeCollectOrangeRegimeDataEventManager;

    @Override
    public void action(MeterDataQueryResponse message, Message savedMessage) throws BusinessException {
        LOGGER.debug("MeterDataQueryResponse received");

        DocumentType documentType = (message.getQueryType() == MeterDataQueryType.EVENTS) ? DocumentType.METER_DATA_QUERY_EVENTS
                : DocumentType.METER_DATA_QUERY_USAGE;

        // find sent Meter Data Query
        PlanboardMessage planboardMessage = corePlanboardBusinessService
                .findSinglePlanboardMessage(message.getDateRangeStart(), documentType,
                        message.getMessageMetadata().getSenderDomain());

        // if not new, message already processed
        if (DocumentStatus.PROCESSED.equals(planboardMessage.getDocumentStatus())) {
            LOGGER.error("MeterDataQueryResponse of the type {} is no longer expected.", message.getQueryType());
            return;
        }
        // process message
        List<MeterDataSet> meterData = new ArrayList<>();
        if (DispositionSuccessFailure.SUCCESS.equals(message.getResult())) {
            LOGGER.debug("MeterDataQueryResponse SUCCESS");
            meterData = message.getMeterDataSet();
        } else {
            LOGGER.error(
                    "Meter Data Company returned MeterDataQueryResponse with FAILURE, this means we will continue the settlement "
                            + "process with prorated allocation!");
        }

        if (documentType == DocumentType.METER_DATA_QUERY_USAGE) {
            // FinalizeInitiateSettlementEvent
            finalizeInitiateSettlementEventManager
                    .fire(new FinalizeInitiateSettlementEvent(message.getDateRangeStart(), message.getDateRangeEnd(), meterData));
        } else {
            // FinalizeCollectOrangeRegimeDataEvent
            finalizeCollectOrangeRegimeDataEventManager.fire(new FinalizeCollectOrangeRegimeDataEvent(meterData, planboardMessage
                    .getPeriod()));
        }

        planboardMessage.setDocumentStatus(DocumentStatus.PROCESSED);
    }
}
