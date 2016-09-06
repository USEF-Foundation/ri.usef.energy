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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.event.HousekeepingEvent;

/**
 * Restful service for housekeeping services.
 */
@Path("/Event")
public class HousekeepingEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(HousekeepingEndpoint.class);

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @Inject
    private Event<HousekeepingEvent> cleanupEventManager;

    /**
     * Fires a {@Link HousekeepingEvent} for each period indicated.
     *
     * @param period {@link String} first period to clean up.
     * @param days {@link String} representing the number of days to clean up.
     * @return a HTTP {@link Response}
     */
    @GET
    @Path("/Cleanup")
    public Response cleanup(@QueryParam("period") @DefaultValue("2015-11-03") String period,
            @QueryParam("days") @DefaultValue("1") String days) {

        LOGGER.debug("Cleanup ({}, {})", period, days);

        // Validate input parameters
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN);
            new Date(formatter.parse(period).getTime());
        } catch (ParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid period '" + period + "' specified, expected format is '" + DATE_PATTERN + "'.").build();
        }

        Integer numberOfDays;
        try {
            numberOfDays = Integer.parseInt(days);
            if (numberOfDays < 1) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid number of days specified'" + days + "', expected a natural number greater than 0.").build();
        }

        for (int i = 0; i < numberOfDays; i++) {
            cleanupEventManager.fire(new HousekeepingEvent(new LocalDate(period).plusDays(i)));
        }

        return Response.status(Response.Status.OK).build();
    }
}
