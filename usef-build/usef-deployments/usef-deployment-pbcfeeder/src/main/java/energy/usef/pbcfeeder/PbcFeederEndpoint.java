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

package energy.usef.pbcfeeder;

import energy.usef.pbcfeeder.dto.PbcStubDataDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoint to request data from the PBCFeeder.
 */
@Path("/PBCFeeder")
public class PbcFeederEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(PbcFeederEndpoint.class);

    @Inject
    private PbcFeeder pbcFeeder;

    /**
     * Endpoint that returns a list of {@link PbcStubDataDto}.
     *
     * @param dateAsString : use the following format: YYYY-MM-DD.
     * @param startIndex : the ptuindex of the day to start with.
     * @param amount : the amount of PTUs to be generated.
     * @return
     */
    @GET
    @Path("/ptu/{date}/{startIndex}/{amount}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PbcStubDataDto> getPtuRows(@PathParam("date") String dateAsString, @PathParam("startIndex") int startIndex,
            @PathParam("amount") Integer amount) {
        LocalDate date = new LocalDate(dateAsString);
        return pbcFeeder.getStubRowInputList(date, startIndex, amount);
    }

    /**
     * @param id
     * @return
     */
    @GET
    @Path("/congestionpoint/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Double> getCongestionPointLoad(@PathParam("id") Integer id) {
        if (id != null) {
            return pbcFeeder.getUncontrolledLoadForCongestionPoint(id);
        } else {
            return pbcFeeder.getUncontrolledLoadForCongestionPoint(0);
        }
    }

    @GET
    @Path("/apx")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Double> getApx() {
        return pbcFeeder.getApx();
    }

    @GET
    @Path("/pvactual")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Double> getPvActual() {
        return pbcFeeder.getPvActual();
    }

    @GET
    @Path("/pvforecast")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Double> getPvForecast() {
        return pbcFeeder.getPvForecast();
    }

    /**
     * Get the power limits for the specified congestion point (given its id).
     *
     * @param congestionPointId {@link Integer} ID of the congestion point.
     * @return a {@link List} of power limits. First element is the lower limit; second element is the upper limit.
     */
    @GET
    @Path("/powerLimit/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<BigDecimal> getCongestionPointPowerLimits(@PathParam("id") Integer congestionPointId) {
        List<BigDecimal> powerLimits = pbcFeeder.getCongestionPointPowerLimits(congestionPointId);
        LOGGER.debug("Retrieve power limits for congestion [{}]: [{}]", congestionPointId, powerLimits.stream().map(
                BigDecimal::toString).collect(Collectors.joining(", ")));
        return powerLimits;
    }

}
