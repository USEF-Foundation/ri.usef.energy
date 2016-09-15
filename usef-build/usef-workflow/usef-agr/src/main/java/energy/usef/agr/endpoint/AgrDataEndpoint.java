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

import energy.usef.agr.service.business.AgrDataBusinessService;
import energy.usef.agr.workflow.plan.updateforecast.UpdateConnectionForecastEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Path("/Data")
public class AgrDataEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrDataEndpoint.class);

    @Inject
    AgrDataBusinessService service;

    /**
     * Return all {@Link CommonReferenceOperator}s in json format.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/CommonReferenceOperators")
    public Response getCommonReferenceOperatorEndpoint() {
        try {
            LOGGER.info("Received request to get all CommonReferenceOperators");
            return Response.status(Response.Status.OK).entity(service.getCommonReferenceOperators()).build();
        } finally {
            LOGGER.info("Processed request to get all CommonReferenceOperators");
        }
    }

    /**
     * Return all {@Link SynchronisationConnection}s in json format.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/SynchronisationConnections")
    public Response getSynchronisationConnectionsEndpoint() {
        try {
            LOGGER.info("Received request to get all SynchronisationConnections");
            return Response.status(Response.Status.OK).entity(service.getSynchronisationConnections()).build();
        } finally {
            LOGGER.info("Processed request to get all SynchronisationConnections");
        }
    }

    /**
     * Endpoint for json {@link String} containing common reference operator information.
     *
     * @param jsonText a json {@link String} containing update information for {@Link CommonReferenceOperator}s.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/UploadCommonReferenceOperators")
    public Response postCommonReferenceOperators(String jsonText) {
        try {
            LOGGER.info("Received request upload CommonReferenceOperators {}", jsonText);
            service.updateCommonReferenceOperators(jsonText);
            return Response.status(Response.Status.OK).entity("Update CommonReferenceOperators processed.").build();
        } finally {
            LOGGER.info("Processed request upload CommonReferenceOperators {}", jsonText);
        }
    }

    /**
     * Endpoint for json {@link String} containing synchronisation connection information.
     *
     * @param jsonText a json {@link String} containing update information for {@Link SynchronisationConnection}s.
     */

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/UploadSynchronisationConnections")
    public Response postSynchronisationConnections(String jsonText) {
        try {
            LOGGER.info("Received request upload SynchronisationConnections {}", jsonText);
            service.updateSynchronisationConnections(jsonText);
            return Response.status(Response.Status.OK).entity("Update SynchronisationConnections processed.").build();
        } finally {
            LOGGER.info("Processed request upload SynchronisationConnections {}", jsonText);
        }
    }
}
