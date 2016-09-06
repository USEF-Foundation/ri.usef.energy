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

package energy.usef.mdc.controller;

import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.mdc.model.CommonReferenceQueryState;
import energy.usef.mdc.model.CommonReferenceQueryStatus;
import energy.usef.mdc.service.business.MdcCoreBusinessService;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller class handling the reception of a {@link CommonReferenceQueryResponse} message.
 */
@Stateless
public class CommonReferenceQueryResponseController extends BaseIncomingResponseMessageController<CommonReferenceQueryResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonReferenceQueryResponseController.class);

    @Inject
    private MdcCoreBusinessService mdcCoreBusinessService;

    @Override
    public void action(CommonReferenceQueryResponse message, Message savedMessage) throws BusinessException {
        LOGGER.info("Received CommonReferenceQueryResponse message for conversation [{}] with result [{}].",
                message.getMessageMetadata().getConversationID(), message.getResult());

        // in any case, persist the status of the common reference query
        CommonReferenceQueryState commonReferenceQueryState = new CommonReferenceQueryState();
        commonReferenceQueryState.setMessage(savedMessage);
        commonReferenceQueryState.setPeriod(DateTimeUtil.getCurrentDate());

        if (DispositionSuccessFailure.SUCCESS == message.getResult()) {
            mdcCoreBusinessService.storeConnectionsForCommonReferenceOperator(message.getConnection(),
                    message.getMessageMetadata().getSenderDomain());
            commonReferenceQueryState.setStatus(CommonReferenceQueryStatus.SUCCESS);
        } else {
            commonReferenceQueryState.setStatus(CommonReferenceQueryStatus.FAILURE);
        }
        mdcCoreBusinessService.storeCommonReferenceQueryState(commonReferenceQueryState);
    }
}
