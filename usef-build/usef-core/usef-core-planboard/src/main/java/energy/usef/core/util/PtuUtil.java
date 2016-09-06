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

package energy.usef.core.util;

import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.exception.TechnicalException;

import java.util.Collections;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;

/**
 * This utility class provides methods to work with PTU's.
 */
public class PtuUtil {

    private PtuUtil() {
        // private constructor.
    }

    /**
     * Get the number of PTU's for a specific day. The number of PTU's is different when the day switches to winter time or summer
     * time. These switches are defined by the timezone the dateTime is defined for. For example: early in the morning of Sunday,
     * March 28th 2014 the time is switched to summer time. And early in the morning of Sunday, October 26th 2014 the time is
     * switched to winter time. This is based on the timezone Europe/Berlin.
     * 
     * The number of minutes per day is divided by the PTU duration. The outcome is ceiled. So, 96.2 PTU's is ceiled to 97 PTU's.
     * 
     * @param dateTime the datetime which is set. When a default constructor is used to create a DateTime instance, the default
     *            timezone is set (which is system default).
     * @param ptuDuration the duration of a ptu (in minutes)
     * @return the number of ptu's for a specific day.
     */
    public static int getNumberOfPtusPerDay(LocalDate date, int ptuDuration) {
        if (ptuDuration != 0) {
            return (int) Math.ceil(DateTimeUtil.getMinutesOfDay(date) / ptuDuration);
        } else {
            throw new TechnicalException("Invalid configuration for PTU duration.");
        }
    }

    /**
     * Gets PTU index corresponding to a given timestamp.
     * 
     * @param timestamp timestamp
     * @param ptuDuration the duration of a ptu (in minutes)
     * @return the PTU index
     */
    public static int getPtuIndex(LocalDateTime timestamp, int ptuDuration) {
        Integer minutesToday = DateTimeUtil.getElapsedMinutesSinceMidnight(timestamp);
        return 1 + minutesToday / ptuDuration;
    }

    /**
     * Orders a ptu list by start.
     * 
     * @param ptus
     * @return
     */
    public static void orderByStart(List<PTU> ptus) {
        Collections.sort(ptus, (ptu1, ptu2) -> ptu1.getStart().compareTo(ptu2.getStart()));
    }

    /**
     * Return the number of PTU's between two {@link LocalDate}'s based on the ptuDuration and ptuIndex of the end date.
     *
     * @param startDate     Start date for the difference (starting with ptu index 1)
     * @param endDate       End date for the difference (ending at parameter ptuIndex)
     * @param ptuStartIndex The ptu index of the start date
     * @param ptuEndIndex   The ptu index of the end date
     * @param ptuDuration   Number of minutes per PTU
     * @return
     */
    public static Integer numberOfPtusBetween(LocalDate startDate, LocalDate endDate, int ptuStartIndex, int ptuEndIndex,
            int ptuDuration) {
        Double ptusPassed = Math.floor(
                Minutes.minutesBetween(startDate.toLocalDateTime(LocalTime.MIDNIGHT), endDate.toLocalDateTime(LocalTime.MIDNIGHT))
                        .getMinutes() / ptuDuration) - ptuStartIndex + ptuEndIndex;
        return ptusPassed.intValue();
    }

}
