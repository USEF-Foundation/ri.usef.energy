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

package energy.usef.agr.workflow.validate.flexoffer;

import energy.usef.core.data.xml.bean.message.USEFRole;

/**
 * Event to fire the processes in charge the revocation of Flex Offer.
 */
public class FlexOfferRevocationEvent {

    private final Long flexOfferSequenceNumber;
    private final String recipientDomainName;
    private final USEFRole usefRole;

    /**
     * Default constructor.
     * 
     * @param flexOfferSequenceNumber Flex Offer Sequence Number
     * @param recipientDomainName Recipient Domain Name
     * @param usefRole USEF Role
     */
    public FlexOfferRevocationEvent(Long flexOfferSequenceNumber, String recipientDomainName, USEFRole usefRole) {
        this.flexOfferSequenceNumber = flexOfferSequenceNumber;
        this.recipientDomainName = recipientDomainName;
        this.usefRole = usefRole;
    }

    public Long getFlexOfferSequenceNumber() {
        return flexOfferSequenceNumber;
    }

    public String getRecipientDomainName() {
        return recipientDomainName;
    }

    public USEFRole getUsefRole() {
        return usefRole;
    }

    @Override
    public String toString() {
        return "FlexOfferRevocationEvent" + "[" +
                "flexOfferSequenceNumber=" + flexOfferSequenceNumber +
                "]";
    }
}
