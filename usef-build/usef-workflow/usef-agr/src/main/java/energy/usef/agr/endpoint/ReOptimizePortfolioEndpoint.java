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

import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioEvent;

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
 * Restful service to fire off re-optimize portfolio event on-demand.
 */
@Path("/ReOptimizePortfolio")
public class ReOptimizePortfolioEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReOptimizePortfolioEndpoint.class);

    @Inject
    private Event<ReOptimizePortfolioEvent> eventManager;

    /**
     * Triggers Re-optimize Portfolio event with a date as parameter.
     * <p>URL: /ReOptimizePortfolio/yyyy-MM-dd</p>
     *
     * @param date {@LocalDate} the date for which the event should be triggered.
     *
     * @return HTTP response status
     */
    @GET
    @Path("/{date}")
    public Response reOptimizePortfolio(@PathParam("date") String date) {
        LOGGER.info("ReOptimizePortfolio Received {}", date);

        try {
            LocalDate ptuDate = new LocalDate(date);
            eventManager.fire(new ReOptimizePortfolioEvent(ptuDate));
        } catch (IllegalArgumentException e) {
            LOGGER.info("Parsing error for request.", e);
            return createErrorResponse("Can not parse the date");
        }
        return Response.status(Response.Status.OK).entity("Correctly proccessed Re-optimize Portfolio event").build();
    }

    private Response createErrorResponse(String message) {
        return Response.status(BAD_REQUEST).entity(message).build();
    }
}
