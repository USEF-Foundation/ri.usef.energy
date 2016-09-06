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
import energy.usef.core.event.DayAheadClosureEvent;
import energy.usef.core.event.IntraDayClosureEvent;
import energy.usef.core.event.MoveToOperateEvent;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;

import javax.enterprise.event.Event;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link CorePlanboardEventTrigger} class.
 */
@RunWith(PowerMockRunner.class)
public class CorePlanboardEventTriggerTest {

    @Mock
    private CorePlanboardBusinessService corePlanboardService;
    @Mock
    private SchedulerHelperService schedulerService;
    @Mock
    private Event<DayAheadClosureEvent> dayAheadClosureEventManager;
    @Mock
    private Event<IntraDayClosureEvent> intraDayClosureEventManager;
    @Mock
    private Event<MoveToOperateEvent> moveToOperateEventManager;
    @Mock
    private Config config;


    private CorePlanboardEventTrigger corePlanboardEventTrigger;

    private static final int PTU_DURATION = 15;
    private static final int DAY_AHEAD_GATE_CLOSURE_PTUS = 8;
    private static final int INTRADAY_GATE_CLOSURE_PTUS = 4;

    @Before
    public void init() {
        corePlanboardEventTrigger = new CorePlanboardEventTrigger();
        Whitebox.setInternalState(corePlanboardEventTrigger, "config", config);
        Whitebox.setInternalState(corePlanboardEventTrigger, "corePlanboardService", corePlanboardService);
        Whitebox.setInternalState(corePlanboardEventTrigger, "schedulerService", schedulerService);
        Whitebox.setInternalState(corePlanboardEventTrigger, "dayAheadClosureEventManager", dayAheadClosureEventManager);
        Whitebox.setInternalState(corePlanboardEventTrigger, "intraDayClosureEventManager", intraDayClosureEventManager);
        Whitebox.setInternalState(corePlanboardEventTrigger, "moveToOperateEventManager", moveToOperateEventManager);
        PowerMockito.when(config.getProperty(ConfigParam.HOST_ROLE)).thenReturn("AGR");
        PowerMockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.PTU_DURATION))).thenReturn(PTU_DURATION);
        PowerMockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.DAY_AHEAD_GATE_CLOSURE_PTUS))).thenReturn(
                DAY_AHEAD_GATE_CLOSURE_PTUS);
        PowerMockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.INTRADAY_GATE_CLOSURE_PTUS))).thenReturn(
                INTRADAY_GATE_CLOSURE_PTUS);
        PowerMockito.when(config.getProperty(Matchers.eq(ConfigParam.DAY_AHEAD_GATE_CLOSURE_TIME))).thenReturn("05:00");
    }

    @Test
    public void testRegisterDayAheadClosureEventWithTinyClosureTime() {
        PowerMockito.when(config.getProperty(Matchers.eq(ConfigParam.DAY_AHEAD_GATE_CLOSURE_TIME))).thenReturn("01:00");
        corePlanboardEventTrigger.registerScheduledEvents();
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        Mockito.verify(schedulerService, Mockito.times(1)).registerScheduledCall(Matchers.eq("DAY_AHEAD_GATE_CLOSURE_EVENT"),
                runnableCaptor.capture(),
                Matchers.any(Long.class),
                Matchers.eq(Days.days(1).toStandardDuration().getMillis()));
        runnableCaptor.getValue().execute();
        ArgumentCaptor<DayAheadClosureEvent> eventCaptor = ArgumentCaptor.forClass(DayAheadClosureEvent.class);
        Mockito.verify(dayAheadClosureEventManager, Mockito.times(1)).fire(eventCaptor.capture());

        DayAheadClosureEvent event = eventCaptor.getValue();
        Assert.assertNotNull(event);
        Assert.assertEquals(new LocalDate().plusDays(2), event.getPeriod());
    }

    @Test
    public void testRegisterDayAheadClosureEventWithNormalClosureTime() {
        corePlanboardEventTrigger.registerScheduledEvents();
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        Mockito.verify(schedulerService, Mockito.times(1)).registerScheduledCall(Matchers.eq("DAY_AHEAD_GATE_CLOSURE_EVENT"),
                runnableCaptor.capture(),
                Matchers.any(Long.class),
                Matchers.eq(Days.days(1).toStandardDuration().getMillis()));
        runnableCaptor.getValue().execute();
        ArgumentCaptor<DayAheadClosureEvent> eventCaptor = ArgumentCaptor.forClass(DayAheadClosureEvent.class);
        Mockito.verify(dayAheadClosureEventManager, Mockito.times(1)).fire(eventCaptor.capture());

        DayAheadClosureEvent event = eventCaptor.getValue();
        Assert.assertNotNull(event);
        Assert.assertEquals(new LocalDate().plusDays(1), event.getPeriod());
    }

    @Test
    public void testRegisterIntraDayClosureEvent() {
        corePlanboardEventTrigger.registerScheduledEvents();
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        Mockito.verify(schedulerService, Mockito.times(1)).registerScheduledCall(Matchers.eq("INTRA_DAY_CLOSURE_EVENT"),
                runnableCaptor.capture(),
                Matchers.any(Long.class),
                Matchers.eq(Minutes.minutes(PTU_DURATION).toStandardDuration().getMillis()));
        runnableCaptor.getValue().execute();
        ArgumentCaptor<IntraDayClosureEvent> eventCaptor = ArgumentCaptor.forClass(IntraDayClosureEvent.class);
        Mockito.verify(intraDayClosureEventManager, Mockito.times(1)).fire(eventCaptor.capture());

        IntraDayClosureEvent event = eventCaptor.getValue();
        Assert.assertNotNull(event);
        LocalDateTime intradayClosure = DateTimeUtil.getCurrentDateTime().plusMinutes(PTU_DURATION * INTRADAY_GATE_CLOSURE_PTUS);
        LocalDate period = intradayClosure.toLocalDate();
        Integer ptuIndex = PtuUtil.getPtuIndex(intradayClosure, PTU_DURATION);

        Assert.assertEquals(period, event.getPeriod());
        Assert.assertEquals(ptuIndex.intValue(), event.getPtuIndex().intValue());
    }

    @Test
    @PrepareForTest({ DateTimeUtil.class, PtuUtil.class })
    public void testMoveToOperateEvent() {
        LocalDateTime currentDateTime = new LocalDateTime(2015, 11, 2, 16, 0, 0);
        LocalTime currentTime = currentDateTime.toLocalTime();

        PowerMockito.mockStatic(DateTimeUtil.class);
        PowerMockito.mockStatic(PtuUtil.class);

        PowerMockito.when(DateTimeUtil.getCurrentDateTime()).thenReturn(currentDateTime);
        PowerMockito.when(DateTimeUtil.getCurrentTime()).thenReturn(currentTime);
        PowerMockito.when(DateTimeUtil.millisecondDelayUntilNextTime(Matchers.anyObject())).thenReturn(900000L);

        PowerMockito.when(PtuUtil.getPtuIndex(Matchers.anyObject(), Matchers.anyInt())).thenReturn(64);

        corePlanboardEventTrigger.registerScheduledEvents();
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        Mockito.verify(schedulerService, Mockito.times(1)).registerScheduledCall(Matchers.eq("MOVE_TO_OPERATE_EVENT"),
                runnableCaptor.capture(),
                Matchers.any(Long.class),
                Matchers.eq(Minutes.minutes(PTU_DURATION).toStandardDuration().getMillis()));
        runnableCaptor.getValue().execute();
        ArgumentCaptor<MoveToOperateEvent> eventCaptor = ArgumentCaptor.forClass(MoveToOperateEvent.class);
        Mockito.verify(moveToOperateEventManager, Mockito.times(1)).fire(eventCaptor.capture());

        MoveToOperateEvent event = eventCaptor.getValue();
        Assert.assertNotNull("Event", event);

        Assert.assertEquals("PtuDate", eventDate(2015, 11, 2, 16, 0, 0), event.getPeriod());
        Assert.assertEquals("PtuIndex", 64, event.getPtuIndex().intValue());
    }

    @Test
    public void testMDCRoleDoesNotRegisterTriggers() {
        PowerMockito.when(config.getProperty(ConfigParam.HOST_ROLE)).thenReturn("MDC");
        corePlanboardEventTrigger.registerScheduledEvents();
        Mockito.verify(schedulerService, Mockito.times(0)).registerScheduledCall(
                Matchers.any(String.class),
                Matchers.any(WorkItemExecution.class),
                Matchers.any(Long.class),
                Matchers.any(Long.class));
    }

    private static LocalDate eventDate(int year, int month, int day, int hours, int minutes, int seconds) {
        return new LocalDateTime(year, month, day, hours, minutes, seconds).plusSeconds(30).toLocalDate();
    }

}
