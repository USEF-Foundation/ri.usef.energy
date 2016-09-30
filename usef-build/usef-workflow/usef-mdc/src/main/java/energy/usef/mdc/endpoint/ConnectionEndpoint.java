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
package energy.usef.mdc.endpoint;

import energy.usef.core.util.JsonUtil;
import energy.usef.mdc.service.business.MeterDataCompanyTopologyBusinessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/connections")
public class ConnectionEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionEndpoint.class);

    @Inject
    MeterDataCompanyTopologyBusinessService service;

    /**
     * Endpoint to get all {@Link Connection} objects.
     *
     * @return a {@Link Response} message containing the requested information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllConnections() {
        LOGGER.info("Received request to get all Connection");
        try {
            return Response.ok(JsonUtil.createJsonText(service.findAllConnections()), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed request to get all Connection");
        }
    }

    /**
     * Endpoint to get an {@Link Connection} givenit's entityAddress.
     *
     * @param entityAddress {@link String} containing the entityAddress of the {@Link Connection}
     * @return a {@Link Response} message containing the requested information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{entityAddress}")
    public Response getConnection(@PathParam("entityAddress") String entityAddress) {
        LOGGER.info("Received request to get Connection {}", entityAddress);
        try {
            return Response.ok(JsonUtil.createJsonText(service.findConnection(entityAddress)), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed request to get Connection {}", entityAddress);
        }
    }

    /**
     * Endpoint to post a batch of {@Link Connection} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link Connection} updates.
     * @return a {@Link Response} message containing batch update results
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postConnectionUpdateBatch(String jsonText) {
        try {
            LOGGER.info("Received update batch for Connections {}", jsonText);
            return Response.ok(JsonUtil.createJsonText(service.processConnectionBatch(jsonText)), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException | com.github.fge.jsonschema.core.exceptions.ProcessingException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed update batch for Connections  {}", jsonText);
        }
    }
}
