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
package energy.usef.cro.endpoint;

import energy.usef.core.util.JsonUtil;
import energy.usef.cro.service.business.CommonReferenceOperatorTopologyBusinessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/aggregators")
public class AggregatorEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatorEndpoint.class);

    @Inject
    CommonReferenceOperatorTopologyBusinessService service;

    /**
     * Endpoint to get all {@Link Aggregator} objects.
     *
     * @return a {@Link Response} message containing the requested information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAggregators() {
        LOGGER.info("Received request to get all Aggregators");
        try {
            return Response.ok(JsonUtil.createJsonText(service.findAllAggregators()), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed request to get all Aggregators");
        }
    }

    /**
     * Endpoint to get an {@Link Aggregator} givenit's domain name.
     *
     * @param domain {@link String} containing the domain name of the {@Link Aggregator}
     * @return a {@Link Response} message containing the requested information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{domain}")
    public Response getAggegatorByDomain(@PathParam("domain") String domain) {
        LOGGER.info("Received request to get Aggregator {}", domain);
        try {
            return Response.ok(JsonUtil.createJsonText(service.findAggregator(domain)), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed request to get Aggregator {}", domain);
        }
    }

    /**
     * Endpoint to post a batch of {@Link Aggregator} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link Aggregator} updates.
     * @return a {@Link Response} message containing batch update results
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postAggregatorUpdateBatch(String jsonText) {
        try {
            LOGGER.info("Received update batch for Aggregators {}", jsonText);
            return Response.ok(JsonUtil.createJsonText(service.processAggregatorBatch(jsonText)), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException | com.github.fge.jsonschema.core.exceptions.ProcessingException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed update batch for Aggregators  {}", jsonText);
        }
    }
}
