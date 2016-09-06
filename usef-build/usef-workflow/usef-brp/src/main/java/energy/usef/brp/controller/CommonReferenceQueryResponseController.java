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

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;
import energy.usef.core.service.business.CorePlanboardBusinessService;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller class in charge of the reception of a {@link CommonReferenceQueryResponse} message for a BRP participant.
 */
@Stateless
public class CommonReferenceQueryResponseController extends BaseIncomingResponseMessageController<CommonReferenceQueryResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonReferenceQueryResponseController.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private ConfigBrp configBrp;

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(CommonReferenceQueryResponse message, Message savedMessage) throws BusinessException {
        LOGGER.info("Received a CommonReferenceQueryResponse from [{}] with result [{}].",
                message.getMessageMetadata().getSenderDomain(), message.getResult().name());
        if (DispositionSuccessFailure.SUCCESS.equals(message.getResult())) {
            LOGGER.info("Common Reference Query response will be stored.");

            Integer initializationDelay = configBrp.getIntegerProperty(ConfigBrpParam.BRP_INITIALIZE_PLANBOARD_DAYS_AHEAD);
            Integer initializationDuration = configBrp.getIntegerProperty(ConfigBrpParam.BRP_INITIALIZE_PLANBOARD_DAYS_INTERVAL);
            LocalDate initializationDate = findFirstMessageOfConversation(message).getCreationTime()
                    .toLocalDate()
                    .plusDays(initializationDelay);
            corePlanboardBusinessService.storeCommonReferenceQueryResponse(message, CommonReferenceEntityType.AGGREGATOR,
                    initializationDate, initializationDuration);
        }
    }
}
