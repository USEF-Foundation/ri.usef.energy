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

import energy.usef.agr.endpoint.dto.ConnectionRestDto;
import energy.usef.agr.workflow.plan.updateforecast.UpdateConnectionForecastEvent;
import energy.usef.core.model.Connection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the endpoint to start the workflow to update the connection portfolio of the AGR. A list of connection IDs can be passed
 * to the endpoint, if no connections are passed ALL the connection portfolios have to get updated.
 */
@Path("/connectionportfolio/update")
public class UpdateConnectionForecastEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateConnectionForecastEndpoint.class);

    @Inject
    private Event<UpdateConnectionForecastEvent> eventManager;

    /**
     * Update one single connection forecast by passing a connection id.
     * <p>URL: /connectionportfolio/update/ea.31341414</p>
     *
     * @param connectionEntityAddress is the id of a {@link Connection}
     * @return a http-response with code 200 in case of success.
     */
    @GET
    @Path("/{connectionEntityAddress}")
    public Response updateConnectionForecast(@PathParam("connectionEntityAddress") String connectionEntityAddress) {
        LOGGER.info("Received connection update request for connectionEntityAddress: {}", connectionEntityAddress);
        List<String> connections = new ArrayList<>();
        connections.add(connectionEntityAddress);
        eventManager.fire(new UpdateConnectionForecastEvent(Optional.of(connections)));
        LOGGER.info("UpdateConnectionForecastEvent fired, event ended.");
        return Response.status(Response.Status.OK).entity("Update connection processed.").build();
    }

    /**
     * When calling this GET-request all Connection forecasts of the aggregator will be updated.
     * <p>URL: /connectionportfolio/update/all</p>
     * 
     * @return a http code 200 response in case of success.
     */
    @GET
    @Path("/all")
    public Response updateAllConnectionForecasts() {
        LOGGER.info("Received connection update request for all connection forecasts.");
        eventManager.fire(new UpdateConnectionForecastEvent());
        LOGGER.info("UpdateConnectionForecastEvent fired for all Connections.");
        return Response.status(Response.Status.OK).entity("Update all connection forecasts processed.").build();
    }

    /**
     * This method receives a POST-request with a JSON-object containing a list of connection Ids to be updated.
     * <p>URL: /connectionportfolio/update</p>
     *
     *
     * @param connectionDto Connection Dto
     * @return a http-response with code 200 in case of success, if not all IDs can be found it will return a HTTP 400 - BAD REQUEST
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateMultipleConnectionForecasts(ConnectionRestDto connectionDto) {
        LOGGER.info("Received connection update request for #{} connections.", connectionDto.getConnectionEntityAddressList()
                .size());
        eventManager.fire(new UpdateConnectionForecastEvent(Optional.of(connectionDto.getConnectionEntityAddressList())));
        LOGGER.info("Update events fired for list of multiple events.");
        return Response.status(Response.Status.OK).entity("Update list of connection forecasts processed.").build();
    }

}
