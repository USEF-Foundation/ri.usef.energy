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

import energy.usef.brp.workflow.settlement.initiate.FinalizeInitiateSettlementEvent;
import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.data.xml.bean.message.MeterDataQueryResponse;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;

import java.util.List;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.LocalDateTime;
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
    private Event<FinalizeInitiateSettlementEvent> eventManager;

    @Override
    public void action(MeterDataQueryResponse message, Message savedMessage) throws BusinessException {
        LOGGER.debug("MeterDataQueryResponse received");

        // find sent Meter Data Queries
        List<PlanboardMessage> meterDataQueryUsageMessages = corePlanboardBusinessService
                .findPlanboardMessages(DocumentType.METER_DATA_QUERY_USAGE, message.getDateRangeStart(), DocumentStatus.SENT);

        // filter the latest sent which is not expired
        LocalDateTime now = DateTimeUtil.getCurrentDateTime();
        PlanboardMessage meterDataQueryUsageMessage = meterDataQueryUsageMessages.stream()
                .filter(meterDataQuery -> !meterDataQuery.getExpirationDate().isBefore(now))
                .sorted((query1, query2) -> query1.getCreationDateTime().isAfter(query2.getCreationDateTime()) ? -1 : 1)
                .findFirst().orElse(null);

        // if not new, message already processed
        if (meterDataQueryUsageMessage == null) {
            LOGGER.error("MeterDataQueryResponse is not or no longer expected as settlement has already started.");
            return;
        }

        // process message
        if (DispositionSuccessFailure.SUCCESS.equals(message.getResult())) {
            LOGGER.debug("MeterDataQueryResponse SUCCESS");

            // FinalizeInitiateSettlementEvent
            eventManager.fire(new FinalizeInitiateSettlementEvent(message.getDateRangeStart(), message.getDateRangeEnd(),
                    message.getMeterDataSet()));
            meterDataQueryUsageMessage.setDocumentStatus(DocumentStatus.PROCESSED);
        } else {
            meterDataQueryUsageMessage.setDocumentStatus(DocumentStatus.REJECTED);
            LOGGER.error("Meter Data Company returned MeterDataQueryResponse with FAILURE. Unable to continue!");
        }
    }
}
