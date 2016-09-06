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

package energy.usef.core.adapter;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;

/**
 * This is an Adapter to map xsd types to YodaTime types.
 */
public class YodaTimeAdapter {

    private YodaTimeAdapter() {
        // private method
    }

    /**
     * Transforms a String (xsd:dateTime) to a {@link LocalDateTime}.
     * 
     * @param dateTimeString A String (xsd:dateTime)
     * @return {@link LocalDateTime}
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        if (StringUtils.isNotEmpty(dateTimeString)) {
            return new DateTime(dateTimeString).withZone(DateTimeZone.getDefault()).toLocalDateTime();
        }
        return null;
    }

    /**
     * Transforms a {@link LocalDateTime} to a String (xsd:dateTime).
     * 
     * @param date {@link LocalDateTime}
     * @return String (xsd:dateTime)
     */
    public static String printDateTime(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return date.toString();
    }

    /**
     * Transforms a String (xsd:time) to a {@link LocalDateTime}.
     * 
     * @param timeString A String (xsd:time)
     * @return String (xsd:time)
     */
    public static LocalTime parseTime(String timeString) {
        if (StringUtils.isNotEmpty(timeString)) {
            return new LocalTime(timeString);
        }
        return null;
    }

    /**
     * Transforms a {@link LocalTime} to a String (xsd:time).
     * 
     * @param date {@link LocalTime}
     * @return String (xsd:time)
     */
    public static String printTime(LocalTime date) {
        if (date == null) {
            return null;
        }
        return date.toString();
    }

    /**
     * Transforms a String (xsd:date) to a {@link LocalDate}.
     * 
     * @param dateString A String (xsd:date)
     * @return {@link LocalDate}
     */
    public static LocalDate parseDate(String dateString) {
        if (StringUtils.isNotEmpty(dateString)) {
            return new LocalDate(dateString);
        }
        return null;
    }

    /**
     * Transforms a {@link LocalDate} to a String (xsd:dateTime) .
     * 
     * @param date {@link LocalDate}
     * @return String (xsd:date)
     */
    public static String printDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.toString();
    }

    /**
     * Transforms a String (xsd:duration) to a {@link LocalDate}.
     * 
     * @param periodString A String (xsd:duration)
     * @return {@link Period}
     */
    public static Period parseDuration(String periodString) {
        if (StringUtils.isNotEmpty(periodString)) {
            return new Period(periodString);
        }
        return null;
    }

    /**
     * Transforms a {@link Period} to a String (xsd:duration) .
     * 
     * @param period {@link Period}
     * @return String (xsd:duration)
     */
    public static String printDuration(Period period) {
        if (period == null) {
            return null;
        }
        return period.toString();
    }
}
