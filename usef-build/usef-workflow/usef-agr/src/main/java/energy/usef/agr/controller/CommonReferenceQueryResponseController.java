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

package energy.usef.agr.controller;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.workflow.plan.connection.profile.AgrUpdateElementDataStoreEvent;
import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.XMLUtil;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes common reference update response.
 */
@Startup
@Singleton
public class CommonReferenceQueryResponseController extends BaseIncomingResponseMessageController<CommonReferenceQueryResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonReferenceQueryResponseController.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private Event<AgrUpdateElementDataStoreEvent> agrUpdateElementDataStoreEventManager;

    /**
     * {@inheritDoc}
     */
    @Lock(LockType.WRITE)
    public void action(CommonReferenceQueryResponse message, Message savedMessage) throws BusinessException {
        LOGGER.debug("CommonReferenceQueryResponse received");
        if (DispositionSuccessFailure.SUCCESS.equals(message.getResult())) {
            LOGGER.info("Store CommonReferenceQueryResponse");
            // we now that the common reference query is not null
            Message commonReferenceQuery = findFirstMessageOfConversation(message);
            CommonReferenceEntityType type = fetchEntityTypeFromOriginalQuery(commonReferenceQuery);

            Integer initializationDelay = 1;
            Integer initializationDuration = configAgr.getIntegerProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL);
            LocalDate initializationDate = commonReferenceQuery.getCreationTime().toLocalDate().plusDays(initializationDelay);
            // store the common reference query response.
            corePlanboardBusinessService.storeCommonReferenceQueryResponse(message, type, initializationDate,
                    initializationDuration);
            // fire event to populate profile values for connection portfolio if every response has been received.
            if (messageService.hasEveryCommonReferenceQuerySentAResponseReceived(commonReferenceQuery.getCreationTime())) {
                LOGGER.debug("Every CommonReferenceQuery has a related Response for period [{}].", initializationDate);
                // Update the element data store
                agrUpdateElementDataStoreEventManager.fire(new AgrUpdateElementDataStoreEvent(initializationDate));
            }
        }
        LOGGER.debug("CommonReferenceQueryResponse finished");
    }

    private CommonReferenceEntityType fetchEntityTypeFromOriginalQuery(Message commonReferenceQuery) {
        return XMLUtil.xmlToMessage(commonReferenceQuery.getXml(), CommonReferenceQuery.class).getEntity();
    }

}
