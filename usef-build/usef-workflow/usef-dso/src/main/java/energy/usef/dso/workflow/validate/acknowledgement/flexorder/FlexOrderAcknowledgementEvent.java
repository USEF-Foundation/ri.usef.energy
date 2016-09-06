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

package energy.usef.dso.workflow.validate.acknowledgement.flexorder;

import energy.usef.core.model.AcknowledgementStatus;

/**
 * Event class used to trigger the 'Flew Order Acknowledgement' DSO workflow.
 */
public class FlexOrderAcknowledgementEvent {

    private Long sequence;
    private AcknowledgementStatus acknowledgementStatus;
    private final String aggregatorDomain;

    /**
     * Specific constructor for the {@link FlexOrderAcknowledgementEvent}.
     * 
     * @param sequence sequence number of the related flexibility request.
     * @param acknowledgementStatus acknowledgement status
     * @param aggregatorDomain
     */
    public FlexOrderAcknowledgementEvent(Long sequence, AcknowledgementStatus acknowledgementStatus, String aggregatorDomain) {
        this.sequence = sequence;
        this.acknowledgementStatus = acknowledgementStatus;
        this.aggregatorDomain = aggregatorDomain;
    }

    public Long getSequence() {
        return sequence;
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
                "sequence=" + sequence +
                ", acknowledgementStatus=" + acknowledgementStatus +
                ", aggregatorDomain='" + aggregatorDomain + "'" +
                "]";
    }
}
