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
package energy.usef.core.event.validation;

import energy.usef.core.event.ExpirableEvent;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.util.DateTimeUtil;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for eventValidationService
 */
public class EventValidationServiceTest {
    private EventValidationService eventValidationService;

    @Before
    public void init() {
        eventValidationService = new EventValidationService();
    }

    @Test(expected = BusinessValidationException.class)
    public void validateEventPeriodTodayOrInFutureNull() throws BusinessValidationException {
        ExpirableTestEvent event = new ExpirableTestEvent(null);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);
    }

    @Test(expected = BusinessValidationException.class)
    public void validateEventPeriodTodayOrInFutureYesterday() throws BusinessValidationException {
        ExpirableTestEvent event = new ExpirableTestEvent(DateTimeUtil.getCurrentDate().minusDays(1));
        eventValidationService.validateEventPeriodTodayOrInFuture(event);
    }

    @Test
    public void validateEventPeriodTodayOrInFutureToday() {
        ExpirableTestEvent event = new ExpirableTestEvent(DateTimeUtil.getCurrentDate());
        try {
            eventValidationService.validateEventPeriodTodayOrInFuture(event);
        } catch (BusinessValidationException e) {
            Assert.fail("No BusinessValidationException expected");
        }
    }

    @Test
    public void validateEventPeriodTodayOrInFutureTomorrow() {
        ExpirableTestEvent event = new ExpirableTestEvent(DateTimeUtil.getCurrentDate().plusDays(1));
        try {
            eventValidationService.validateEventPeriodTodayOrInFuture(event);
        } catch (BusinessValidationException e) {
            Assert.fail("No BusinessValidationException expected");
        }
    }

    @Test(expected = BusinessValidationException.class)
    public void validateEventPeriodInFutureNull() throws BusinessValidationException {
        ExpirableTestEvent event = new ExpirableTestEvent(null);
        eventValidationService.validateEventPeriodInFuture(event);
    }

    @Test(expected = BusinessValidationException.class)
    public void validateEventPeriodInFutureYesterday() throws Exception {
        ExpirableTestEvent event = new ExpirableTestEvent(DateTimeUtil.getCurrentDate().minusDays(1));
        eventValidationService.validateEventPeriodInFuture(event);
    }

    @Test(expected = BusinessValidationException.class)
    public void validateEventPeriodInFutureToday() throws Exception {
        ExpirableTestEvent event = new ExpirableTestEvent(DateTimeUtil.getCurrentDate());
        eventValidationService.validateEventPeriodInFuture(event);
    }

    @Test
    public void validateEventPeriodInFutureTomorrow() {
        ExpirableTestEvent event = new ExpirableTestEvent(DateTimeUtil.getCurrentDate().plusDays(1));
        try {
            eventValidationService.validateEventPeriodInFuture(event);
        } catch (BusinessValidationException e) {
            Assert.fail("No BusinessValidationException expected");
        }
    }

    @Test(expected = BusinessValidationException.class)
    public void validateEventPeriodTodayOrInPastNull() throws BusinessValidationException {
        ExpirableTestEvent event = new ExpirableTestEvent(null);
        eventValidationService.validateEventPeriodTodayOrInPast(event);
    }

    @Test
    public void validateEventPeriodTodayOrInPastYesterday() throws Exception {
        ExpirableTestEvent event = new ExpirableTestEvent(DateTimeUtil.getCurrentDate().minusDays(1));
        try {
            eventValidationService.validateEventPeriodTodayOrInPast(event);
        } catch (BusinessValidationException e) {
            Assert.fail("No BusinessValidationException expected");
        }
    }

    @Test
    public void validateEventPeriodTodayOrInPastToday() throws Exception {
        ExpirableTestEvent event = new ExpirableTestEvent(DateTimeUtil.getCurrentDate());
        try {
            eventValidationService.validateEventPeriodTodayOrInPast(event);
        } catch (BusinessValidationException e) {
            Assert.fail("No BusinessValidationException expected");
        }
    }

    @Test(expected = BusinessValidationException.class)
    public void validateEventPeriodTodayOrInPastTomorrow() throws Exception {
        ExpirableTestEvent event = new ExpirableTestEvent(DateTimeUtil.getCurrentDate().plusDays(1));
        eventValidationService.validateEventPeriodTodayOrInPast(event);
    }

    private class ExpirableTestEvent implements ExpirableEvent {
        private final LocalDate period;

        public ExpirableTestEvent(LocalDate period) {this.period = period;}

        public LocalDate getPeriod() {return period;}
    }
}