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

package energy.usef.brp.endpoint;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import energy.usef.brp.workflow.plan.connection.forecast.PrepareFlexRequestsEvent;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restful service designed to trigger the PrepareFlexRequest PBC.
 */
@Path("/PrepareFlexRequests")
public class PrepareFlexRequestsEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareFlexRequestsEndpoint.class);

    @Inject
    private Event<PrepareFlexRequestsEvent> prepareFlexRequestsEventManager;

    /**
     * Fires a new FlexRequestEvent.
     * <p>URL: /PrepareFlexRequests/yyyy-MM-dd</p>
     *
     * 
     * @param date {@LocalDate} the date for which the event should be triggered.
     * 
     * @return a HTTP {@link Response}.
     */
    @GET
    @Path("/{date}")
    public Response prepareFlexRequestsEvent(@PathParam("date") String date) {
        LOGGER.info("PrepareFlexRequests Received {}", date);
        try {
            LocalDate period = new LocalDate(date);
            prepareFlexRequestsEventManager.fire(new PrepareFlexRequestsEvent(period));
            return Response.status(Response.Status.OK).entity("PrepareFlexRequests Received").build();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Parsing error for request.", e);
            return createErrorResponse("Can not parse the date");
        }
    }

    private Response createErrorResponse(String message) {
        return Response.status(BAD_REQUEST).entity(message).build();
    }

}
