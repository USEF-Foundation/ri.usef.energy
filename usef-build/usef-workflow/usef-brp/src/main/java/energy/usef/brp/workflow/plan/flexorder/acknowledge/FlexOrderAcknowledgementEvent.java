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

package energy.usef.brp.workflow.plan.flexorder.acknowledge;

import energy.usef.core.model.AcknowledgementStatus;

/**
 * Event class for triggering the flex order acknowledgement workflow (BRP side).
 */
public class FlexOrderAcknowledgementEvent {

    private final Long flexOrderSequence;
    private final AcknowledgementStatus acknowledgementStatus;
    private final String aggregatorDomain;

    /**
     * Constructor of the event.
     *
     * @param flexOrderSequence {@link Long} sequence number of the flex order to work on.
     * @param acknowledgementStatus {@link AcknowledgementStatus} for the flex order.
     * @param aggregatorDomain
     */
    public FlexOrderAcknowledgementEvent(Long flexOrderSequence, AcknowledgementStatus acknowledgementStatus,
            String aggregatorDomain) {
        this.flexOrderSequence = flexOrderSequence;
        this.acknowledgementStatus = acknowledgementStatus;
        this.aggregatorDomain = aggregatorDomain;
    }

    public Long getFlexOrderSequence() {
        return flexOrderSequence;
    }

    public AcknowledgementStatus getAcknowledgementStatus() {
        return acknowledgementStatus;
    }

    public String getAggregatorDomain() {
        return aggregatorDomain;
    }

    @Override
    public String toString() {
        return "FlexOrderAcknowledgementEvent" + "[" +
                "flexOrderSequence=" + flexOrderSequence +
                ", acknowledgementStatus=" + acknowledgementStatus +
                ", aggregatorDomain='" + aggregatorDomain + "'" +
                "]";
    }
}
