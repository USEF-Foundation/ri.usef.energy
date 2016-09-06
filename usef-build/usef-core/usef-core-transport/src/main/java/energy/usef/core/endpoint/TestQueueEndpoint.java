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

package energy.usef.core.endpoint;

import energy.usef.core.service.helper.JMSHelperService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Restful service. Test tool.
 */
@Path("/TestQueueEndpoint")
public class TestQueueEndpoint {
    @Inject
    private JMSHelperService jmsHelperService;

    /**
     * Sends a message to the In queue.
     *
     * @param message message.
     * @return status
     */
    @POST
    @Path("/sentToInQueue")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response sentToInQueue(String message) {
        jmsHelperService.sendMessageToInQueue(message);
        return Response.status(Response.Status.OK).entity("Correctly send to In Queue").build();
    }

    /**
     * Sends a message to the Out queue.
     *
     * @param message message.
     * @return status
     */
    @POST
    @Path("/sentToOutQueue")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response sentToOutQueue(String message) {
        jmsHelperService.sendMessageToOutQueue(message);
        return Response.status(Response.Status.OK).entity("Correctly send to Out Queue").build();
    }

}
