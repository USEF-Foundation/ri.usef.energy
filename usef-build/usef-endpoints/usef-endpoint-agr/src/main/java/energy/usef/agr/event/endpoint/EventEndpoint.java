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

package energy.usef.agr.event.endpoint;

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

import energy.usef.agr.workflow.nonudi.goals.AgrNonUdiSetAdsGoalsEvent;
import energy.usef.agr.workflow.nonudi.initialize.AgrNonUdiInitializeEvent;
import energy.usef.agr.workflow.nonudi.operate.AgrNonUdiRetrieveAdsGoalRealizationEvent;
import energy.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyEvent;
import energy.usef.agr.workflow.operate.deviation.DetectDeviationEvent;
import energy.usef.agr.workflow.operate.identifychangeforecast.IdentifyChangeInForecastEvent;
import energy.usef.agr.workflow.operate.netdemand.DetermineNetDemandEvent;
import energy.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesEvent;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizeFlagHolder;
import energy.usef.agr.workflow.plan.commonreferenceupdate.CommonReferenceUpdateEvent;
import energy.usef.agr.workflow.plan.connection.forecast.CommonReferenceQueryEvent;
import energy.usef.agr.workflow.plan.connection.forecast.CreateConnectionForecastEvent;
import energy.usef.agr.workflow.plan.connection.profile.CreateConnectionProfileEvent;
import energy.usef.agr.workflow.plan.create.aplan.CreateAPlanEvent;
import energy.usef.agr.workflow.plan.create.aplan.FinalizeAPlansEvent;
import energy.usef.agr.workflow.settlement.initiate.InitiateSettlementEvent;
import energy.usef.agr.workflow.settlement.receive.CheckSettlementEvent;
import energy.usef.agr.workflow.validate.create.dprognosis.CreateDPrognosisEvent;
import energy.usef.agr.workflow.validate.flexoffer.FlexOfferEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.MoveToOperateEvent;
import energy.usef.core.event.StartValidateEvent;
import energy.usef.core.util.DateTimeUtil;

/**
 * Restful service to send events to the aggregator.
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
    private Event<CommonReferenceQueryEvent> commonReferenceQueryEventManager;

    @Inject
    private Event<ControlActiveDemandSupplyEvent> cadsEventManager;

    @Inject
    private Event<DetectDeviationEvent> detectDeviationEventManager;

    @Inject
    private Event<CreateDPrognosisEvent> createDPrognosisEventManager;

    @Inject
    private Event<InitiateSettlementEvent> initiateSettlementEventManager;

    @Inject
    private Event<CheckSettlementEvent> checkSettlementEventEvent;

    @Inject
    private Event<CreateAPlanEvent> createAPlanEventManager;

    @Inject
    private Event<FinalizeAPlansEvent> updateAPlanEventManager;

    @Inject
    private Event<FlexOfferEvent> flexOfferEventManager;

    @Inject
    private Event<ReCreatePrognosesEvent> reCreatePrognosesEventManager;

    @Inject
    private Event<IdentifyChangeInForecastEvent> identifyChangeInForecastEventManager;

    @Inject
    private Event<MoveToOperateEvent> moveToOperateEventManager;

    @Inject
    private Event<StartValidateEvent> startValidateEventManager;

    @Inject
    private Event<DetermineNetDemandEvent> determineNetDemandEventEventManager;

    @Inject
    private ReOptimizeFlagHolder reOptimizeFlagHolder;

    @Inject
    private Event<AgrNonUdiInitializeEvent> agrNonUdiInitializeEventManager;

    @Inject
    private Event<AgrNonUdiRetrieveAdsGoalRealizationEvent> agrNonUdiRetrieveAdsGoalRealizationEventManager;

    @Inject
    private Event<AgrNonUdiSetAdsGoalsEvent> agrNonUdiSetAdsGoalsEventManager;

    @Inject
    private Event<CreateConnectionProfileEvent> createConnectionProfileEventManager;

    /**
     * Turn on or off the scheduler. The values true/false, 0/1 or on/off can be used.
     *
     * @param onOrOff which is a string and can be true/false, 0/1 or on/off. When the value is unknown, the scheduler is turned
     * on.
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
     * @return {@link Response}
     */
    @GET
    @Path("/CommonReferenceQueryEvent")
    public Response sendCommonReferenceQueryEvent() {
        LOGGER.info("ConnectionForecastEvent fired.");
        commonReferenceQueryEventManager.fire(new CommonReferenceQueryEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a {@link ControlActiveDemandSupplyEvent}.
     *
     * @return {@link Response}
     */
    @GET
    @Path("/ControlActiveDemandSupplyEvent")
    public Response sendControlActiveDemandSupplyEvent() {
        LOGGER.info("ControlActiveDemandSupplyEvent fired.");
        cadsEventManager.fire(new ControlActiveDemandSupplyEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a {@link DetectDeviationEvent} for a specific period.
     *
     * @param period the period
     * @return {@link Response}
     */
    @GET
    @Path("/DetectDeviationEvent/{period}")
    public Response sendDetectDeviationEvent(@PathParam("period") String period) {
        LOGGER.info("DetectDeviationEvent fired.");
        detectDeviationEventManager.fire(new DetectDeviationEvent(new LocalDate(period)));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a {@link CreateDPrognosisEvent}.
     *
     * @return {@link Response}
     */
    @GET
    @Path("/CreateDPrognosisEvent")
    public Response sendCreateDPrognosisEvent() {
        LOGGER.info("CreateDPrognosisEvent fired.");
        createDPrognosisEventManager.fire(new CreateDPrognosisEvent(null, null));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a {@link CreateDPrognosisEvent} for a given period and a given congestion point.
     *
     * @param period {@link String} period of creation.
     * @param congestionPoint {@link String} usef identifier of the congestion point.
     * @return {@link Response}
     */
    @GET
    @Path("/CreateDPrognosisEvent/{period}/{congestionPoint}")
    public Response sendCreateDPrognosisEventForPeriodForCongestionPoint(@PathParam("period") String period,
            @PathParam("congestionPoint") String congestionPoint) {
        LOGGER.info("CreateDPrognosisEvent fired.");
        createDPrognosisEventManager.fire(new CreateDPrognosisEvent(new LocalDate(period), congestionPoint));
        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("/InitiateSettlementEvent/{ptuDate}")
    public Response sendInitiateSettlementEvent(@PathParam("ptuDate") String ptuDate) {
        LOGGER.info("InitiateSettlementEvent fired.");

        InitiateSettlementEvent event = new InitiateSettlementEvent();
        if (StringUtils.isNotEmpty(ptuDate)) {
            event.setPeriodInMonth(new LocalDate(ptuDate));
        }
        initiateSettlementEventManager.fire(event);
        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("/CheckSettlementEvent/{ptuDate}")
    public Response sendCheckSettlementEvent(@PathParam("ptuDate") String ptuDate) {
        LOGGER.info("InitiateSettlementEvent fired.");

        CheckSettlementEvent event = new CheckSettlementEvent();
        if (StringUtils.isNotEmpty(ptuDate)) {
            event.setPeriodInMonth(new LocalDate(ptuDate));
        }
        checkSettlementEventEvent.fire(event);
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a {@link CreateAPlanEvent}.
     *
     * @param periodValue the period when the A-Plan is created. This is a date in the format YYYY-MM-DD.
     * @return {@link Response}
     */
    @GET
    @Path("/CreateAPlanEvent/{period}")
    public Response sendCreateAPlanEvent(@PathParam("period") String periodValue) {
        LOGGER.info("CreateAPlanEvent fired.");
        createAPlanEventManager.fire(new CreateAPlanEvent(new LocalDate(periodValue), null));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a {@link FinalizeAPlansEvent}.
     *
     * @param periodValue the period when the A-Plan is created. This is a date in the format YYYY-MM-DD.
     * @return {@link Response}
     */
    @GET
    @Path("/UpdateAPlanEvent/{period}")
    public Response sendUpdateAPlanEvent(@PathParam("period") String periodValue) {
        LOGGER.info("UpdateAPlanEvent fired.");
        updateAPlanEventManager.fire(new FinalizeAPlansEvent(new LocalDate(periodValue)));
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
     * Fires a {@link FlexOfferEvent}.
     *
     * @return {@link Response}
     */
    @GET
    @Path("/FlexOfferEvent")
    public Response sendFlexOfferEvent() {
        LOGGER.info("FlexOfferEvent fired.");
        flexOfferEventManager.fire(new FlexOfferEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a {@link IdentifyChangeInForecastEvent}.
     *
     * @return {@link Response}
     */
    @GET
    @Path("/IdentifyChangeInForecastEvent")
    public Response sendIdentifyChangeInForecastEvent() {
        LOGGER.info("IdentifyChangeInForecastEvent fired.");
        identifyChangeInForecastEventManager.fire(new IdentifyChangeInForecastEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires the {@link ReCreatePrognosesEvent} with the specified usef
     * identifier.
     *
     * @param periodString {@link String} Period for which one wants to trigger the workflow (yyyy-MM-dd).
     * @return a HTTP {@link javax.ws.rs.core.Response}.
     */
    @GET
    @Path("/ReCreatePrognosesEvent/{period}")
    public Response fireReCreatePrognosesEvent(@PathParam("period") String periodString) {
        LOGGER.info("ReCreatePrognosesEvent fired.");
        reCreatePrognosesEventManager.fire(new ReCreatePrognosesEvent(new LocalDate(periodString)));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires the {@link MoveToOperateEvent}.
     *
     * @param periodString {@link String} Period for which one wants to trigger the workflow (yyyy-MM-dd).
     * @param ptuIndex PTU Index
     * @return a HTTP {@link javax.ws.rs.core.Response}.
     */
    @GET
    @Path("/MoveToOperateEvent/{period}/{ptuIndex}")
    public Response fireMoveToOperateEvent(@PathParam("period") String periodString, @PathParam("ptuIndex") String ptuIndex) {
        LOGGER.info("MoveToOperateEvent fired.");
        moveToOperateEventManager.fire(new MoveToOperateEvent(new LocalDate(periodString), Integer.valueOf(ptuIndex)));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires an event to start the validate phase for a given period.
     *
     * @param periodString {@link String} period (yyyy-MM-dd)
     * @return a HTTP {@link Response}
     */
    @GET
    @Path("/StartValidatePhase/{period}")
    public Response fireStartValidatePhase(@PathParam("period") String periodString) {
        LOGGER.info("StartValidatePhase event fired.");
        startValidateEventManager.fire(new StartValidateEvent(new LocalDate(periodString)));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires an DetermineNetDemandEvent.
     *
     * @return
     */
    @GET
    @Path("/DetermineNetDemandEvent")
    public Response fireDetermineNetDemandEvent() {
        determineNetDemandEventEventManager.fire(new DetermineNetDemandEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires the initialize powermatcher clusters event for aggregators using the non-udi flow.
     *
     * @return a HTTP {@link Response}
     */
    @GET
    @Path("/InitializePowerMatcherEvent")
    public Response fireInitializePowerMatcher() {
        agrNonUdiInitializeEventManager.fire(new AgrNonUdiInitializeEvent(DateTimeUtil.getCurrentDate()));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires a new {@link AgrNonUdiRetrieveAdsGoalRealizationEvent} event.
     *
     * @return a HTTP {@link Response}.
     */
    @GET
    @Path("/NonUdiRetrieveAdsGoalRealization")
    public Response fireAgrNonUdiRetrieveAdsGoalRealizationEvent() {
        LOGGER.info("Firing a new AgrNonUdiRetrieveAdsGoalRealizationEvent.");
        agrNonUdiRetrieveAdsGoalRealizationEventManager.fire(new AgrNonUdiRetrieveAdsGoalRealizationEvent());
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Resets the flags of the the {@link ReOptimizeFlagHolder} to false for the given period.
     *
     * @param periodString {@link String} period as a String (yyyy-MM-dd)
     * @return a HTTP {@link Response}
     */
    @GET
    @Path("/ReOptimizePortfolioState/reset/{period}")
    public Response resetReOptimizePortfolioFlags(@PathParam("period") String periodString) {
        reOptimizeFlagHolder.setIsRunning(new LocalDate(periodString), false);
        reOptimizeFlagHolder.setToBeReoptimized(new LocalDate(periodString), false);
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires the set ADS goals via PowerMatcher event for aggregators using the non-udi flow.
     *
     * @param periodString {@link String} period as a String (yyyy-MM-dd).
     * @param usefIdentifier {@link String} containing a CongestionPoint or BRP id.
     * @return a HTTP {@link Response}
     */
    @GET
    @Path("/SetAdsGoalsEvent/{period}/{usefIdentifier}")
    public Response fireSetAdsGoalsEvent(@PathParam("period") String periodString,
            @PathParam("usefIdentifier") String usefIdentifier) {
        agrNonUdiSetAdsGoalsEventManager.fire(new AgrNonUdiSetAdsGoalsEvent(new LocalDate(periodString), usefIdentifier));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Fires the create connection profile event.
     *
     * @param periodString {@link String} period as a String (yyyy-MM-dd).
     * @return a HTTP {@link Response}
     */
    @GET
    @Path("/CreateConnectionProfileEvent/{period}")
    public Response fireCreateConnectionProfileEvent(@PathParam("period") String periodString) {
        createConnectionProfileEventManager.fire(new CreateConnectionProfileEvent(new LocalDate(periodString)));
        return Response.status(Response.Status.OK).build();
    }

}
