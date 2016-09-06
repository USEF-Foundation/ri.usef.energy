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

package energy.usef.core.timer;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.event.DayAheadClosureEvent;
import energy.usef.core.event.IntraDayClosureEvent;
import energy.usef.core.event.MoveToOperateEvent;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.Hours;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;

/**
 * This class registers scheduled events which will trigger updates in the planboard when fired.
 */
@Singleton
@Startup
public class CorePlanboardEventTrigger {

    public static final int PTU_OFFSET_IN_SECONDS = 30;
    private static final int TWO_DAYS = 2;
    @Inject
    private SchedulerHelperService schedulerService;
    @Inject
    private Event<DayAheadClosureEvent> dayAheadClosureEventManager;
    @Inject
    private Event<IntraDayClosureEvent> intraDayClosureEventManager;
    @Inject
    private Event<MoveToOperateEvent> moveToOperateEventManager;
    @Inject
    private CorePlanboardBusinessService corePlanboardService;
    @Inject
    private Config config;

    /**
     * This method is called when the application is started. It will register triggers to fire {@link DayAheadClosureEvent},
     * {@link IntraDayClosureEvent} and {@link MoveToOperateEvent} on a regular basis.
     */
    @PostConstruct
    public void registerScheduledEvents() {
        if (USEFRole.MDC.name().equals(config.getProperty(ConfigParam.HOST_ROLE))) {
            return;
        }
        corePlanboardService.initialisePtuContainers();
        registerDayAheadClosureEventTimer();
        registerIntraDayClosureEventTimer();
        registerMoveToOperateEventTimer();
        
    }

    private void registerDayAheadClosureEventTimer() {
        final LocalTime dayAheadClosureTime = new LocalTime(config.getProperty(ConfigParam.DAY_AHEAD_GATE_CLOSURE_TIME));

        final Integer dayAheadClosureOffsetPtus = config.getIntegerProperty(ConfigParam.DAY_AHEAD_GATE_CLOSURE_PTUS);
        final LocalTime dayAheadClosureOffsetTime = dayAheadClosureTime.minus(
                Minutes.minutes(config.getIntegerProperty(ConfigParam.PTU_DURATION) * dayAheadClosureOffsetPtus));
        schedulerService.registerScheduledCall("DAY_AHEAD_GATE_CLOSURE_EVENT", () -> {
            LocalDateTime todayDayAheadClosureTime = DateTimeUtil.getCurrentDateWithTime(dayAheadClosureTime);
            if (dayAheadClosureOffsetPtus > PtuUtil.getPtuIndex(todayDayAheadClosureTime,
                    config.getIntegerProperty(ConfigParam.PTU_DURATION))) {
                dayAheadClosureEventManager.fire(new DayAheadClosureEvent(DateTimeUtil.getCurrentDate().plusDays(TWO_DAYS)));
            } else {
                dayAheadClosureEventManager.fire(new DayAheadClosureEvent(null));
            }
        }, DateTimeUtil.millisecondDelayUntilNextTime(dayAheadClosureOffsetTime), TimeUnit.DAYS.toMillis(1));

    }

    private void registerIntraDayClosureEventTimer() {
        final Integer intraDayClosurePtus = config.getIntegerProperty(ConfigParam.INTRADAY_GATE_CLOSURE_PTUS);
        final Integer ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        LocalTime nextPtuShiftTime = getNextPtuShiftTime();
        schedulerService.registerScheduledCall("INTRA_DAY_CLOSURE_EVENT", () -> {
            LocalDateTime intradayClosure = DateTimeUtil.getCurrentDateTime().plusMinutes(ptuDuration * intraDayClosurePtus);
            LocalDate period = intradayClosure.toLocalDate();
            Integer ptuIndex = PtuUtil.getPtuIndex(intradayClosure, ptuDuration);
            intraDayClosureEventManager.fire(new IntraDayClosureEvent(period, ptuIndex));
        }, DateTimeUtil.millisecondDelayUntilNextTime(nextPtuShiftTime), Minutes.minutes(ptuDuration).toStandardDuration()
                .getMillis());
    }

    private void registerMoveToOperateEventTimer() {
        final int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);


        LocalTime nextPtuShiftTime = getNextPtuShiftTime();
        schedulerService.registerScheduledCall("MOVE_TO_OPERATE_EVENT", () -> {
            LocalDateTime timestamp = DateTimeUtil.getCurrentDateTime().plusSeconds(PTU_OFFSET_IN_SECONDS);
            Integer ptuIndex = PtuUtil.getPtuIndex(timestamp, ptuDuration);
            moveToOperateEventManager.fire(new MoveToOperateEvent(timestamp.toLocalDate(), ptuIndex));
        }, DateTimeUtil.millisecondDelayUntilNextTime(nextPtuShiftTime), Minutes.minutes(ptuDuration).toStandardDuration()
                .getMillis());
    }

    /**
     * Get the local time for the next ptu shift, using the current time and the ptu duration.
     *
     * @return the {@link LocalTime} of the next PTU shift.
     */
    private LocalTime getNextPtuShiftTime() {
        LocalTime now = DateTimeUtil.getCurrentTime();
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);
        int minutesPerHour = Hours.ONE.toStandardMinutes().getMinutes();
        return new LocalTime(
                (int) ((now.getHourOfDay() + ((now.getMinuteOfHour() / ptuDuration + 1) * ptuDuration / minutesPerHour))
                % TimeUnit.DAYS.toHours(1)),
                ((now.getMinuteOfHour() / ptuDuration + 1) * ptuDuration) % minutesPerHour);
    }
}
