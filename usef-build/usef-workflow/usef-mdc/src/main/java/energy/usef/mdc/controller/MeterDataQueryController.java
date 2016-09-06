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

import energy.usef.core.controller.BaseIncomingMessageController;
import energy.usef.core.data.xml.bean.message.MeterDataQuery;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;
import energy.usef.mdc.workflow.meterdata.MeterDataQueryEvent;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * This controller handle's the {@link MeterDataQuery} message.
 */
@Stateless
public class MeterDataQueryController extends BaseIncomingMessageController<MeterDataQuery> {

    @Inject
    private Event<MeterDataQueryEvent> eventManager;

    @Override
    public void action(MeterDataQuery message, Message savedMessage) throws BusinessException {
        eventManager.fire(new MeterDataQueryEvent(message));
    }
}
