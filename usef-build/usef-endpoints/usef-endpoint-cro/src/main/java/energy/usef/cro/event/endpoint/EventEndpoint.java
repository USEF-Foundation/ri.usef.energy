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

package energy.usef.cro.event.endpoint;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restful service to send events to the CRO. 
 */
@Path("/Event")
public class EventEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventEndpoint.class);

    @Inject
    private Config config;

    /**
     * Turn on or off the scheduler. The values true/false, 0/1 or on/off can be used.
     * @param onOrOff which is a string and can be true/false, 0/1 or on/off. When the value is unknown, the
     *      scheduler is turned on.
     * @return {@link Response}.
     */
    @GET
    @Path("/Scheduler/{onOrOff}")
    public Response turnOnOrOffScheduler(@PathParam("onOrOff") String onOrOff) {
        boolean turnOff = onOrOff != null && !onOrOff.isEmpty() && 
                ("off".equalsIgnoreCase(onOrOff) || "0".equals(onOrOff) || "false".equalsIgnoreCase(onOrOff));
        LOGGER.info("Schedulers are turned " + (turnOff ? "off" : "on"));
        config.getProperties().setProperty(ConfigParam.BYPASS_SCHEDULED_EVENTS.name(), Boolean.toString(turnOff));
        
        return Response.status(Response.Status.OK).build();
    }

}
