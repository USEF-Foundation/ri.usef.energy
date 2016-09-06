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

package energy.usef.agr.endpoint;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import energy.usef.agr.workflow.validate.flexoffer.FlexOfferRevocationEvent;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restful service designed to invoke the Flex Offer Revocation workflow.
 */
@Path("/FlexOfferRevocationEndpoint")
public class FlexOfferRevocationEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlexOfferRevocationEndpoint.class);

    @Inject
    private Event<FlexOfferRevocationEvent> eventManager;

    @Inject
    private CorePlanboardBusinessService planboardBusinessService;

    /**
     * Triggers Flex Offer Revocation workflow.
     * <p>URL: /FlexOfferRevocationEndpoint/revokeFlexOffer/12346788/brp.usef-example.com/BRP</p>
     *
     * @param flexOfferSequence   sequence number of the flex offer that will be revoked
     * @param recipientDomainName domain of the recipient
     * @param recipientRole       role of the recipient
     * @return HTTP response status
     */
    @GET
    @Path("/revokeFlexOffer/{flexOfferSequence}/{recipientDomainName}/{recipientRole}")
    public Response revokeFlexOffer(@PathParam("flexOfferSequence") String flexOfferSequence,
            @PathParam("recipientDomainName") String recipientDomainName, @PathParam("recipientRole") String recipientRole) {
        LOGGER.debug("Received revokeFlexOffer event {}", flexOfferSequence);

        if (recipientDomainName == null || "".equals(recipientDomainName.trim())) {
            return createErrorResponse("No recipientDomain");
        }

        USEFRole usefRole = USEFRole.fromValue(recipientRole);
        if (usefRole == null) {
            return createErrorResponse("Invalid recipientRole");
        }

        try {

            Response response = findAndValidateFlexOfferMessages(flexOfferSequence, recipientDomainName);
            if (response != null) {
                return response;
            }
            Long flexOfferSequenceNumber = Long.parseLong(flexOfferSequence);
            FlexOfferRevocationEvent event = new FlexOfferRevocationEvent(flexOfferSequenceNumber, recipientDomainName, usefRole);
            eventManager.fire(event);
        } catch (NumberFormatException e) {
            return createErrorResponse("Can not parse flexOfferSequenceNumber");
        }

        return Response.status(Response.Status.OK).entity("Correctly processed revoke message").build();
    }

    private Response findAndValidateFlexOfferMessages(String flexOfferSequence, String recipientDomainName) {
        Long flexOfferSequenceNumber = Long.parseLong(flexOfferSequence);
        PlanboardMessage flexOfferMessage = planboardBusinessService.findSinglePlanboardMessage(
                flexOfferSequenceNumber, DocumentType.FLEX_OFFER, recipientDomainName);
        if (flexOfferMessage == null) {
            LOGGER.error("No corresponding plan board message record found for the flex offer sequence number: {}",
                    flexOfferSequence);
            return createErrorResponse("No corresponding plan board message record found for the flex offer sequence number: "
                    + flexOfferSequence);
        }

        if (DocumentStatus.ACCEPTED != flexOfferMessage.getDocumentStatus()) {
            LOGGER.error(
                    "Related plan board message for the flex offer sequence number: {} was never accepted.",
                    flexOfferSequence);
            return createErrorResponse("Related plan board message for the flex offer sequence number: "
                    + flexOfferSequence + " was never accepted.");
        }
        return null;
    }

    private Response createErrorResponse(String message) {
        return Response.status(BAD_REQUEST).entity(message).build();
    }

}
