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

import energy.usef.brp.workflow.plan.commonreferenceupdate.CommonReferenceUpdateResponseEvent;
import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdateResponse;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;

import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * Incoming message controller to process the {@link CommonReferenceUpdateResponse}.
 */
public class CommonReferenceUpdateResponseController extends BaseIncomingResponseMessageController<CommonReferenceUpdateResponse> {

    @Inject
    private Event<CommonReferenceUpdateResponseEvent> eventManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(CommonReferenceUpdateResponse message, Message savedMessage) throws BusinessException {
        eventManager.fire(new CommonReferenceUpdateResponseEvent(message));
    }

}
