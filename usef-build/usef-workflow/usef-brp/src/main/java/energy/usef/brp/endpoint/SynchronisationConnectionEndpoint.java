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

import energy.usef.brp.service.business.BalanceResponsiblePartyTopologyBusinessService;
import energy.usef.core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/synchronisationconnections")
public class SynchronisationConnectionEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronisationConnectionEndpoint.class);

    @Inject
    BalanceResponsiblePartyTopologyBusinessService service;

    /**
     * Endpoint to get all {@Link SynchronisationConnection} objects.
     *
     * @return a {@Link Response} message containing the requested information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSynchronisationConnections() {
        LOGGER.info("Received request to get all SynchronisationConnection");
        try {
            return Response.ok(JsonUtil.createJsonText(service.findAllSynchronisationConnections()), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed request to get all SynchronisationConnection");
        }
    }

    /**
     * Endpoint to get an {@Link SynchronisationConnection} given it's entityAddress name.
     *
     * @param entityAddress {@link String} containing the entityAddress name of the {@Link SynchronisationConnection}
     * @return a {@Link Response} message containing the requested information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{entityAddress}")
    public Response getSynchronisationConnectionByEntityAddress(@PathParam("entityAddress") String entityAddress) {
        LOGGER.info("Received request to get SynchronisationConnection {}", entityAddress);
        try {
            return Response.ok(JsonUtil.createJsonText(service.findSynchronisationConnection(entityAddress)), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed request to get SynchronisationConnection {}", entityAddress);
        }
    }

    /**
     * Endpoint to post a batch of {@Link SynchronisationConnection} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link SynchronisationConnection} updates.
     * @return a {@Link Response} message containing batch update results
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postSynchronisationConnectionUpdateBatch(String jsonText) {
        try {
            LOGGER.info("Received update batch for SynchronisationConnection {}", jsonText);
            return Response.ok(JsonUtil.createJsonText(service.processSynchronisationConnectionBatch(jsonText)), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException | com.github.fge.jsonschema.core.exceptions.ProcessingException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed update batch for SynchronisationConnection {}", jsonText);
        }
    }
}
