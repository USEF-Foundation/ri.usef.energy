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

import static javax.ws.rs.core.MediaType.TEXT_XML;

import energy.usef.core.constant.USEFLogCategory;
import energy.usef.core.service.helper.JMSHelperService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message restful service. Designed to send USEF messages.
 */
@Path("/MessageService")
public class MessageEndpoint {
    private static final Logger LOGGER_CONFIDENTIAL = LoggerFactory.getLogger(USEFLogCategory.CONFIDENTIAL);

    @Inject
    private JMSHelperService jmsService;

    /**
     * Sends a client message to a queue.
     *
     * @param xmlMessage xml message
     * @return status
     */
    @POST
    @Path("/sendMessage")
    @Consumes(TEXT_XML)
    public Response sendMessage(String xmlMessage) {
        LOGGER_CONFIDENTIAL.debug("Received XML message via HTTP/POST: {}", xmlMessage);
        if(StringUtils.isBlank(xmlMessage)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("No message body").build();
        }
        jmsService.sendMessageToOutQueue(xmlMessage);
        return Response.status(Response.Status.OK).entity("Correctly sent XML message").build();
    }

}
