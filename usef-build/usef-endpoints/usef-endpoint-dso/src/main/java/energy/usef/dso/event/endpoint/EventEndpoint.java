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

package energy.usef.dso.event.endpoint;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.DayAheadClosureEvent;
import energy.usef.dso.workflow.operate.SendOperateEvent;
import energy.usef.dso.workflow.plan.commonreferenceupdate.CommonReferenceUpdateEvent;
import energy.usef.dso.workflow.plan.connection.forecast.CommonReferenceQueryEvent;
import energy.usef.dso.workflow.plan.connection.forecast.CreateConnectionForecastEvent;
import energy.usef.dso.workflow.settlement.collect.InitiateCollectOrangeRegimeDataEvent;
import energy.usef.dso.workflow.settlement.initiate.CollectSmartMeterDataEvent;
import energy.usef.dso.workflow.settlement.initiate.FinalizeUnfinishedInitiateSettlementEvent;
import energy.usef.dso.workflow.settlement.send.SendSettlementMessageEvent;
import energy.usef.dso.workflow.validate.create.flexorder.FlexOrderEvent;
import energy.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestEvent;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restful service to send events to the DSO.
 */
@Path("/Event")
public class EventEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventEndpoint.class);
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @Inject
    private Config config;

    @Inject
    private Event<CreateConnectionForecastEvent> createConnectionForecastEventManager;

    @Inject
    private Event<CommonReferenceUpdateEvent> commonReferenceUpdateEventManager;

    @Inject
    private Event<SendOperateEvent> sendOperateEventManager;

    @Inject
    private Event<CommonReferenceQueryEvent> commonReferenceQueryEventManager;

    @Inject
    private Event<FlexOrderEvent> flexOrderEventManager;

    @Inject
    private Event<CreateFlexRequestEvent> createFlexRequestEventManager;

    @Inject
    private Event<SendSettlementMessageEvent> sendSettlementMessageEventManager;

    @Inject
    private Event<CollectSmartMeterDataEvent> collectSmartMeterDataEventManager;

    @Inject
    private Event<FinalizeUnfinishedInitiateSettlementEvent> finalizeUnfinishedInitiateSettlementEventManager;
    @Inject
    private Event<DayAheadClosureEvent> dayAheadClosureEventEventManager;

    @Inject
    private Event<InitiateCollectOrangeRegimeDataEvent> initiateCollectOrangeRegimeDataEvent;

    /**
     * Turn on or off the scheduler. The values true/false, 0/1 or on/off can be used.
     *
     * @param onOrOff which is a string and can be true/false, 0/1 or on/off. When the value is unknown, the scheduler is turned on.
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

    /**
     * A {@link SendOperateEvent} event is triggered on the DSO.
     *
     * @return {@link Response}.
     */
    @GET
    @Path("/SendOperateEvent")
    public Response sendOperateEvent() {
        LOGGER.info("SendOperateEvent fired.");
        sendOperateEventManager.fire(new SendOperateEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * A {@link CommonReferenceUpdateEvent} event is triggered on the DSO.
     *
     * @return {@link Response}.
     */
    @GET
    @Path("/CommonReferenceUpdateEvent")
    public Response sendCommonReferenceUpdateEvent() {
        LOGGER.info("CommonReferenceUpdateEvent fired.");
        commonReferenceUpdateEventManager.fire(new CommonReferenceUpdateEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * A {@link CommonReferenceQueryEvent} event is triggered.
     *
     * @return {@link Response}.
     */
    @GET
    @Path("/CommonReferenceQueryEvent")
    public Response sendConnectionForecastEvent() {
        LOGGER.info("CommonReferenceQueryEvent fired.");
        commonReferenceQueryEventManager.fire(new CommonReferenceQueryEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * A {@link FlexOrderEvent} event is triggered.
     *
     * @return {@link Response}.
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

        CollectSmartMeterDataEvent event = new CollectSmartMeterDataEvent();
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
     * A {@link CreateFlexRequestEvent} event is triggered.
     *
     * @param entityAddress the entity address of the congestion point
     * @param ptuDateValue the period when the flex requests are analyzed. This is a date of the format YYYY-MM-DD.
     * @param ptuIndexesValue a comma seperated list of index of the ptu's.
     * @return {@link Response}.
     */
    @GET
    @Path("/CreateFlexRequestEvent/{entityAddress}/{ptuDate}/{ptuIndexes}")
    public Response sendCreateFlexRequestEvent(@PathParam("entityAddress") String entityAddress,
            @PathParam("ptuDate") String ptuDateValue, @PathParam("ptuIndexes") String ptuIndexesValue) {
        LocalDate ptuDate = parseDate(ptuDateValue);
        Integer[] ptuIndexes = splitPtuString(ptuIndexesValue);
        LOGGER.info("FlexOrderEvent fired for date {} and PTU's {}.", ptuDate, ptuIndexes);
        createFlexRequestEventManager.fire(new CreateFlexRequestEvent(entityAddress, ptuDate, ptuIndexes));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a {@link CreateConnectionForecastEvent}.
     *
     * @return {@link Response}
     */
    @GET
    @Path("/CreateConnectionForecastEvent")
    public Response createConnectionForecastEvent() {
        createConnectionForecastEventManager.fire(new CreateConnectionForecastEvent());
        LOGGER.info("Create connection forecast Event fired.");
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

    @GET
    @Path("/DayAheadClosureEvent")
    public Response sendDayAheadClosureEvent() {
        dayAheadClosureEventEventManager.fire(new DayAheadClosureEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a {@link InitiateCollectOrangeRegimeDataEvent}.
     *
     * @return {@link Response}.
     */
    @GET
    @Path("/InitiateCollectOrangeRegimeDataEvent")
    public Response sendInitiateCollectOrangeRegimeDataEvent() {
        initiateCollectOrangeRegimeDataEvent.fire(new InitiateCollectOrangeRegimeDataEvent());
        LOGGER.info("InitiateCollectOrangeRegimeDataEvent fired.");
        return Response.status(Response.Status.OK).build();
    }

    private static LocalDate parseDate(String value) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(DATE_PATTERN);
        DateTime dateTime = formatter.parseDateTime(value);
        return dateTime.toLocalDate();
    }

    private static Integer[] splitPtuString(String ptuString) {
        String[] strArray = ptuString.split(",");
        Integer[] ptuIndexes = new Integer[strArray.length];
        for (int i = 0; i < strArray.length; i++) {
            ptuIndexes[i] = Integer.parseInt(strArray[i]);
        }
        return ptuIndexes;
    }

}
