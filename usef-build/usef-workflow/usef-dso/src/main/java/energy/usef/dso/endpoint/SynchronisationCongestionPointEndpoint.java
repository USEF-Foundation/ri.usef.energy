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
package energy.usef.dso.endpoint;

import energy.usef.core.util.JsonUtil;
import energy.usef.dso.service.business.DistributionSystemOperatorTopologyBusinessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/synchronisationcongestionpoints")
public class SynchronisationCongestionPointEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronisationCongestionPointEndpoint.class);

    @Inject
    DistributionSystemOperatorTopologyBusinessService service;

    /**
     * Endpoint to get all {@Link SynchronisationCongestionPoint} objects.
     *
     * @return a {@Link Response} message containing the requested information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSynchronisationCongestionPointEndpoints() {
        LOGGER.info("Received request to get all Synchronisation Congestion Points");
        try {
            return Response.ok(JsonUtil.createJsonText(service.findAllSynchronisationCongestionPoints()), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed request to get all SynchronisationsCongestionPoints");
        }
    }

    /**
     * Endpoint to get an {@Link SynchronisationCongestionPoint} given it's entityAddress name.
     *
     * @param entityAddress {@link String} containing the entityAddress name of the {@Link SynchronisationCongestionPoint}
     * @return a {@Link Response} message containing the requested information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{entityAddress}")
    public Response getSynchronisationCongestionPointEndpointByEntitAddress(@PathParam("entityAddress") String entityAddress) {
        LOGGER.info("Received request to get Synchronisation Congestion Point {}", entityAddress);
        try {
            return Response.ok(JsonUtil.createJsonText(service.findSynchronisationCongestionPoint(entityAddress)), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed request to get Synchronisation CongestionPoint {}", entityAddress);
        }
    }

    /**
     * Endpoint to post a batch of {@Link CongestionPoint} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link CongestionPoint} updates.
     * @return a {@Link Response} message containing batch update results
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postCongestionPointUpdateBatch(String jsonText) {
        try {
            LOGGER.info("Received update batch for Congestion Points {}", jsonText);
            return Response.ok(JsonUtil.createJsonText(service.processSynchronisationCongestionPointBatch(jsonText)), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException | com.github.fge.jsonschema.core.exceptions.ProcessingException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed update batch for Congestion Points  {}", jsonText);
        }
    }
}
