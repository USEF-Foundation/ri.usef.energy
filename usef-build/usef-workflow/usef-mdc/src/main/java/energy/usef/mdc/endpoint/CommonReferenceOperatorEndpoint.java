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

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import energy.usef.core.util.JsonUtil;
import energy.usef.mdc.service.business.MeterDataCompanyTopologyBusinessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/commonreferenceoperators")
public class CommonReferenceOperatorEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonReferenceOperatorEndpoint.class);

    @Inject
    MeterDataCompanyTopologyBusinessService service;

    /**
     * Endpoint to get all {@Link CommonReferenceOperator} objects.
     *
     * @return a {@Link Response} message containing the requested information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllCommonReferenceOperators() {
        LOGGER.info("Received request to get all MeterDataCompanies");
        try {
            return Response.ok(JsonUtil.createJsonText(service.findAllCommonReferenceOperators()), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed request to get all CommonReferenceOperator");
        }
    }

    /**
     * Endpoint to get a {@Link CommonReferenceOperator} given it's domain name.
     *
     * @param domain {@link String} containing the domain name of the {@Link CommonReferenceOperator}
     * @return a {@Link Response} message containing the requested information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{domain}")
    public Response getCommonReferenceOperatorByDomain(@PathParam("domain") String domain) {
        LOGGER.info("Received request to get CommonReferenceOperator {}", domain);
        try {
            return Response.ok(JsonUtil.createJsonText(service.findCommonReferenceOperator(domain)), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed request to get MeterDataCompany {}", domain);
        }
    }

    /**
     * Endpoint to post a batch of {@Link CommonReferenceOperator} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link CommonReferenceOperator} updates.
     * @return a {@Link Response} message containing batch update results
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postCommonReferenceOperatorUpdateBatch(String jsonText) {
        try {
            LOGGER.info("Received update batch for MeterDataCompanies {}", jsonText);
            return Response.ok(JsonUtil.createJsonText(service.processCommonReferenceOperatorBatch(jsonText)), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException | ProcessingException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed update batch for MeterDataCompanies");
        }
    }
}
