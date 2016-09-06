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

package energy.usef.agr.workflow.settlement.receive;

import energy.usef.core.data.xml.bean.message.SettlementMessage;
import energy.usef.core.model.Message;

/**
 * Processe's the receiving of the Settlement.
 */
public class ReceiveSettlementMessageEvent {

    private SettlementMessage message;
    private Message savedMessage;

    /**
     * Receive Settlement Message Event.
     *
     * @param message
     * @param savedMessage
     */
    public ReceiveSettlementMessageEvent(SettlementMessage message, Message savedMessage) {
        this.message = message;
        this.savedMessage = savedMessage;
    }

    /**
     * Get the Settlement Message.
     *
     * @return the message
     */
    public SettlementMessage getMessage() {
        return message;
    }

    /**
     * Get the saved Message.
     *
     * @return the savedMessage
     */
    public Message getSavedMessage() {
        return savedMessage;
    }

    @Override
    public String toString() {

        return "ReceiveSettlementMessageEvent" + "[" +
                "sender=" + savedMessage.getSender() +
                "periodStart=" + message.getPeriodStart() +
                "periodEnd=" + message.getPeriodEnd() +
                "]";
    }
}
