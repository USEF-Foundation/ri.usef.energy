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

package energy.usef.brp.event.endpoint;

import energy.usef.brp.workflow.plan.aplan.finalize.FinalizeAPlansEvent;
import energy.usef.brp.workflow.plan.commonreferenceupdate.CommonReferenceUpdateEvent;
import energy.usef.brp.workflow.plan.connection.forecast.CommonReferenceQueryEvent;
import energy.usef.brp.workflow.plan.connection.forecast.ReceivedAPlanEvent;
import energy.usef.brp.workflow.plan.flexorder.place.FlexOrderEvent;
import energy.usef.brp.workflow.settlement.initiate.CollectSmartMeterDataEvent;
import energy.usef.brp.workflow.settlement.initiate.FinalizeUnfinishedInitiateSettlementEvent;
import energy.usef.brp.workflow.settlement.send.SendSettlementMessageEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.DayAheadClosureEvent;
import energy.usef.core.util.DateTimeUtil;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restful service to send events to the brp.
 */
@Path("/Event")
public class EventEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventEndpoint.class);

    @Inject
    private Config config;

    @Inject
    private Event<CommonReferenceUpdateEvent> commonReferenceUpdateEventManager;
    @Inject
    private Event<CommonReferenceQueryEvent> commonReferenceQueryEventManager;
    @Inject
    private Event<FlexOrderEvent> flexOrderEventManager;
    @Inject
    private Event<SendSettlementMessageEvent> sendSettlementMessageEventManager;
    @Inject
    private Event<CollectSmartMeterDataEvent> collectSmartMeterDataEventManager;
    @Inject
    private Event<FinalizeUnfinishedInitiateSettlementEvent> finalizeUnfinishedInitiateSettlementEventManager;
    @Inject
    private Event<ReceivedAPlanEvent> receivedAPlanEventManager;
    @Inject
    private Event<FinalizeAPlansEvent> finalizeAPlansEventManager;
    @Inject
    private Event<DayAheadClosureEvent> dayAheadClosureEventEventManager;

    /**
     * Turn on or off the scheduler. The values true/false, 0/1 or on/off can be used.
     *
     * @param onOrOff which is a string and can be true/false, 0/1 or on/off. When the value is unknown, the scheduler is turned on.
     * @return a HTTP-OK response.
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

    /**
     * Fires a {@link CommonReferenceUpdateEvent}.
     *
     * @return {@link Response}
     */
    @GET
    @Path("/CommonReferenceUpdateEvent")
    public Response sendCommonReferenceUpdateEvent() {
        LOGGER.info("CommonReferenceUpdateEvent fired.");
        commonReferenceUpdateEventManager.fire(new CommonReferenceUpdateEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a {@link CommonReferenceQueryEvent}.
     *
     * @return a HTTP {@link Response}.
     */
    @GET
    @Path("/CommonReferenceQueryEvent")
    public Response sendCommonReferenceQueryEvent() {
        LOGGER.info("CommonReferenceQueryEvent fired.");
        commonReferenceQueryEventManager.fire(new CommonReferenceQueryEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a new FlexOrderEvent.
     *
     * @return a HTTP {@link Response}.
     */
    @GET
    @Path("/FlexOrderEvent")
    public Response sendFlexOrderEvent() {
        LOGGER.info("FlexOrderEvent fired.");
        flexOrderEventManager.fire(new FlexOrderEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * A {@link CollectSmartMeterDataEvent} event is triggered.
     *
     * @return {@link Response}.
     */
    @GET
    @Path("/InitiateSettlementEvent/{ptuDate}")
    public Response sendInitiateSettlementEvent(@PathParam("ptuDate") String ptuDate) {
        LOGGER.info("InitiateSettlementEvent fired.");

        CollectSmartMeterDataEvent event = new CollectSmartMeterDataEvent(null);
        if (StringUtils.isNotEmpty(ptuDate)) {
            event.setPeriodInMonth(new LocalDate(ptuDate));
        }
        collectSmartMeterDataEventManager.fire(event);
        return Response.status(Response.Status.OK).build();
    }

    /**
     * A {@link FinalizeUnfinishedInitiateSettlementEvent} event is triggered.
     *
     * @return {@link Response}.
     */
    @GET
    @Path("/FinalizeUnfinishedInitiateSettlementEvent")
    public Response sendFinalizeUnfinishedInitiateSettlementEvent() {
        LOGGER.info("FinalizeUnfinishedInitiateSettlementEvent fired.");
        finalizeUnfinishedInitiateSettlementEventManager.fire(new FinalizeUnfinishedInitiateSettlementEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a {@link SendSettlementMessageEvent} with the given parameters.
     *
     * @param year {@link Integer} year of the event.
     * @param month {@link Integer} month of the event.
     * @return {@link Response}.
     */
    @GET
    @Path("/SendSettlementMessageEvent/{year}/{month}")
    public Response sendSendSettlementMessageEvent(@PathParam("year") Integer year, @PathParam("month") Integer month) {
        sendSettlementMessageEventManager.fire(new SendSettlementMessageEvent(year, month));
        LOGGER.info("SendSettlementMessageEvent fired for year {} and month {}.", year, month);
        return Response.status(Response.Status.OK).build();
    }

    /**
     * A {@link DayAheadClosureEvent} event is triggered.
     *
     * @return {@link Response}.
     */
    @GET
    @Path("/DayAheadClosureEvent")
    public Response sendDayAheadClosureEvent() {
        dayAheadClosureEventEventManager.fire(new DayAheadClosureEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires the ReceivedAPlanEvent.
     *
     * @param periodString {@link String} date with format yyyy-MM-dd
     * @return HTTP {@link Response}.
     */
    @GET
    @Path("/ReceivedAPlanEvent/{period}")
    public Response sendReceivedAPlanEvent(@PathParam("period") String periodString) {
        receivedAPlanEventManager.fire(new ReceivedAPlanEvent(DateTimeUtil.parseDate(periodString)));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires the FinalizeAPlansEvent.
     *
     * @param periodString {@link String} date with format yyyy-MM-dd
     * @return HTTP {@link Response}.
     */
    @GET
    @Path("/FinalizeAPlansEvent/{period}")
    public Response finalizeAPlansEvent(@PathParam("period") String periodString) {
        finalizeAPlansEventManager.fire(new FinalizeAPlansEvent(DateTimeUtil.parseDate(periodString)));
        return Response.status(Response.Status.OK).build();
    }

}
