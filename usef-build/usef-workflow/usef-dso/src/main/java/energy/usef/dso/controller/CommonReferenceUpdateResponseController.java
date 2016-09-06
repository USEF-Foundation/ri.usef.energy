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

import energy.usef.core.controller.BaseIncomingMessageController;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdateResponse;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;
import energy.usef.dso.workflow.plan.commonreferenceupdate.CommonReferenceUpdateResponseEvent;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes common reference update response.
 */
@Stateless
public class CommonReferenceUpdateResponseController extends BaseIncomingMessageController<CommonReferenceUpdateResponse> {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(CommonReferenceUpdateResponseController.class);

    @Inject
    private Event<CommonReferenceUpdateResponseEvent> eventManager;

    /**
     * {@inheritDoc}
     */
    public void action(CommonReferenceUpdateResponse message, Message savedMessage) throws BusinessException {
        LOGGER.info("CommonReferenceUpdateResponse received");
        eventManager.fire(new CommonReferenceUpdateResponseEvent(message));
    }

}
