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

import javax.enterprise.context.Dependent;

import energy.usef.core.event.ExpirableEvent;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.util.DateTimeUtil;

/**
 * Service to validate events
 */
@Dependent
public class EventValidationService {

    /**
     * Validate if period is not in the past.
     *
     * @param event
     * @throws BusinessValidationException
     */
    public void validateEventPeriodTodayOrInFuture(ExpirableEvent event) throws BusinessValidationException {
        validateEventPeriod(event);
        if(event.getPeriod().isBefore(DateTimeUtil.getCurrentDate())){
            throw new BusinessValidationException(EventError.PERIOD_IN_PAST, event.getClass().getSimpleName());
        }
    }

    /**
     * Validate if period is not today or in the past.
     *
     * @param event
     * @throws BusinessValidationException
     */
    public void validateEventPeriodInFuture(ExpirableEvent event) throws BusinessValidationException {
        validateEventPeriod(event);
        if(event.getPeriod().isBefore(DateTimeUtil.getCurrentDate()) || event.getPeriod().isEqual(DateTimeUtil.getCurrentDate())){
            throw new BusinessValidationException(EventError.PERIOD_TODAY_OR_IN_PAST, event.getClass().getSimpleName());
        }
    }

    /**
     * Validate if period is not in the future.
     *
     * @param event
     * @throws BusinessValidationException
     */
    public void validateEventPeriodTodayOrInPast(ExpirableEvent event) throws BusinessValidationException {
        validateEventPeriod(event);
        if(event.getPeriod().isAfter(DateTimeUtil.getCurrentDate())){
            throw new BusinessValidationException(EventError.PERIOD_IN_FUTURE, event.getClass().getSimpleName());
        }
    }

    /**
     * Validate if period is today.
     *
     * @param event
     * @throws BusinessValidationException
     */
    public void validateEventPeriodToday(ExpirableEvent event) throws BusinessValidationException {
        validateEventPeriod(event);
        if(!event.getPeriod().isEqual(DateTimeUtil.getCurrentDate())){
            throw new BusinessValidationException(EventError.PERIOD_IN_PAST_OR_FUTURE, event.getClass().getSimpleName());
        }
    }

    /**
     * Validate if the period is not null.
     *
     * @param event
     * @throws BusinessValidationException
     */
    public void validateEventPeriod(ExpirableEvent event) throws BusinessValidationException {
        if (event.getPeriod() == null){
            throw new BusinessValidationException(EventError.INVALID_PERIOD, event.getClass().getSimpleName());
        }
    }
}
