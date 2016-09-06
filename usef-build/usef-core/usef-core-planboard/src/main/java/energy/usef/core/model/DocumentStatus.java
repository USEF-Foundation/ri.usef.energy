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

package energy.usef.core.model;

/**
 * Enumeration of the different statuses for a {@link PlanboardMessage} entity.
 */
public enum DocumentStatus {

    /**
     * The planboardMessage gets the status ACCEPTED or REJECTED when the message is received.
     */
    ACCEPTED,
    REJECTED,

    /**
     * The planboardMessage gets the status EXPIRED when the message is expired based on the expiration date.
     */
    EXPIRED,

    /**
     * The planboardMessage gets the status REVOKED when the message explicitly revoked.
     */
    REVOKED,

    /**
     * The planboardMessage gets the status DISPUTED when the message is being explicitly disputed by the other participant.
     */
    RECEIVED,
    DISPUTED,
    OVERDUE,

    /**
     * The planboardMessage gets the status PROCESSED when the message is processed. For instance, when an offer is processed and is
     * decided to make the offer an order or to reject the offer. When the planboardMessage is processed, it will not be processed
     * again.
     */
    PROCESSED,

    /**
     * The planboardMessage gets this status when the workflow who created it needs to be invoked again to create it again (for
     * D-Prognosis, for instance).
     */
    TO_BE_RECREATED,

    /**
     * The planboardMessage gets the status SENT when the message is sent. For instance, when an A-Plan is sent.
     */
    SENT,
    /**
     * An A-Plan gets the status PENDING_FLEX_TRADING when a Flex Request based on that A-Plan was sent to an aggregator.
     */
    PENDING_FLEX_TRADING,
    /**
     * The planboardMessage gets the status FINAL when the message becomes final. For instance, when an A-Plan becomes final.
     */
    FINAL,

    /**
     * The planboardMessage gets the status ARCHIVED when the message becomes archived. For instance, when an A-Plan is recreated,
     * the original A-Plans becomes archived.
     */
    ARCHIVED,

    /**
     * The planboardMessage of type FLEX_REQUEST gets RECEIVED_EMPTY_OFFER when there is received an empty flex offer (no ptu data).
     */
    RECEIVED_EMPTY_OFFER,

    /**
     * The planboardMessage of type FLEX_REQUEST gets RECEIVED_OFFER when there is received an valid flex offer (with ptu data).
     */
    RECEIVED_OFFER
}
