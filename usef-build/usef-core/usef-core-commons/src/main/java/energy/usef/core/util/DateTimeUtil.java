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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utility class provides methods to work with dates.
 */
public class DateTimeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateTimeUtil.class);

    // number of ms that time will be cached.
    private static final int BUFFER_TIME = 200;
    private static final int PACKAGE_BUFFER = 128;
    private static final int RESPONSE_TIMEOUT = 100;

    private static final int MAX_ERROR_COUNT = 10;
    // messages to request data
    private static final String UDP_TIME = "TIME";
    private static final String UDP_TIMEFACTOR = "TIMEFACTOR";

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    public static final int SECONDS_PER_MINUTE = 60;

    private static volatile long timeFactor = 1;
    private static volatile int errorCount = 0;

    private static boolean useServer = false;

    private static String serverIp;
    private static Integer port;

    private static LocalDateTime latestUDPDateTime;
    private static LocalDateTime lastProcessed;

    private DateTimeUtil() {
        // private Constructor
    }

    /**
     * Set the ip and port of the application. Returns true or false whether it is succesfully using the new settings. In case of
     * incorrect parameters, default time will be used. If time and timefactor cannot be retrieved the system will shut down.
     *
     * @param serverIp - The server IP of the usef-time server.
     * @param port - The server port of the usef-time server.
     * @return - true if succesfull.
     */
    public static boolean updateSettings(String serverIp, Integer port) {
        if (StringUtils.isNotEmpty(serverIp) && port != null) {
            DateTimeUtil.serverIp = serverIp;
            DateTimeUtil.port = port;
            DateTimeUtil.useServer = true;
            boolean updatedTimeFactor = false;
            while (!updatedTimeFactor) {
                String udpTimeFactor = getUDPInfo(UDP_TIMEFACTOR);
                if (udpTimeFactor != null && 0 < (DateTimeUtil.timeFactor = Long.valueOf(udpTimeFactor))) {
                    updatedTimeFactor = true;
                }
            }
            while (latestUDPDateTime != null) {
                getTime();
            }
        } else {
            DateTimeUtil.useServer = false;
        }
        return useServer;
    }

    /**
     * Creates a LocalDateTime set to the start of the given date (date at 00:00:00.000)
     *
     * @param date - the date for which to determine the start of day.
     * @return LocalDateTime set to the start of the given date.
     */
    public static LocalDateTime getStartOfDay (LocalDate date) {
        return date.toLocalDateTime(new LocalTime()).withTime(0, 0, 0, 0);
    }

    /**
     * Creates a LocalDateTime set to the end of the given date (next date at 00:00:00.000)
     *
     * @param date - the date for which to determine the end of day.
     * @return LocalDateTime set to the end of the given date.
     */
    public static LocalDateTime getEndOfDay (LocalDate date) {
        return getStartOfDay(date).plusDays(1);
    }

    /**
     * Creates a LocalDateTime set to the start of the given date time (date/time at 00:00:00.000)
     *
     * @param dateTime - the date/time for which to determine the start of day.
     * @return LocalDateTime set to the start of the given date.
     */
    public static LocalDateTime getStartOfDay (LocalDateTime dateTime) {
        return getStartOfDay(dateTime.toLocalDate());
    }
    /**
     * Creates a LocalDateTime set to the end of the given date/time (next date at 00:00:00.000)
     *
     * @param dateTime - the date/time for which to determine the end of day.
     * @return LocalDateTime set to the end of the given date.
     */
    public static LocalDateTime getEndOfDay (LocalDateTime dateTime) {
        return getStartOfDay(dateTime).plusDays(1);
    }

    /**
     * Creates a LocalDateTime set to the current date.
     *
     * @return LocalDateTime set to the current date.
     */
    public static LocalDateTime getCurrentDateTime() {
        return getTime();
    }

    /**
     * Creates a LocalTime set to the current date.
     *
     * @return LocalTime set to the current date.
     */
    public static LocalTime getCurrentTime() {
        return getTime().toLocalTime();
    }

    /**
     * Creates a LocalDate set to the current date.
     *
     * @return LocalDate set to the current date.
     */
    public static LocalDate getCurrentDate() {
        return getTime().toLocalDate();
    }

    /**
     * Gets the number of milliseconds until the next occurence of the local time (which is the current day or the day after).
     *
     * @param localTime {@link LocalTime}
     * @return the number of milliseconds.
     */
    public static Long millisecondDelayUntilNextTime(LocalTime localTime) {
        LocalDateTime schedule = getCurrentDateWithTime(localTime);
        if (schedule.isBefore(getCurrentDateTime())) {
            schedule = schedule.plusDays(1);
        }
        return new Duration(getCurrentDateTime().toDateTime(), schedule.toDateTime()).getMillis();
    }

    /**
     * Gets a date from a string.
     *
     * @param dateString Date format: <b>yyyy-MM-dd</b>
     * @return A {@link LocalDate} object
     */
    public static LocalDate parseDate(String dateString) {
        return new LocalDate(dateString);
    }

    /**
     * Gets string in format "yyyy-MM-dd" from a LocalDate.
     *
     * @param date {@link LocalDate}
     * @return A {@link String} object
     */
    public static String printDate(LocalDate date) {
        return DateTimeFormat.forPattern(DEFAULT_DATE_FORMAT).print(date);
    }

    /**
     * Gets string in format "yyyy-MM-dd HH:mm:ss.SSS"  from a LocalDateTime.
     *
     * @param dateTime {@link LocalDateTime}
     * @return A {@link String} object
     */
    public static String printDateTime(LocalDateTime dateTime) {
        return DateTimeFormat.forPattern(DEFAULT_DATE_TIME_FORMAT).print(dateTime);
    }

    /**
     * Gets string in given pattern from a LocalDateTime.
     *
     * @param dateTime {@link LocalDateTime}
     * @param pattern {@link String}
     * @return A {@link String} object
     */
    public static String printDateTime(LocalDateTime dateTime, String pattern) {
        return DateTimeFormat.forPattern(pattern).print(dateTime);
    }


    /**
     * Calculate the number of minutes from midnight to now. The number of minutes depends on summer- and winter time.
     *
     * @param dateTime the date time which includes the timezone. When setting the DateTime with the default constructor, the
     * default timezone where the application runs, is used.
     * @return the number of minutes from midnight.
     */
    public static Integer getElapsedMinutesSinceMidnight(LocalDateTime dateTime) {
        DateTime midnight = dateTime.toLocalDate().toDateMidnight().toDateTime();
        Duration duration = new Interval(midnight, dateTime.toDateTime()).toDuration();
        return (int) duration.getStandardSeconds() / SECONDS_PER_MINUTE;
    }

    /**
     * Calculate the number of minutes of a specific date. Only the date part (year, month and day) is used to calculate the number
     * of minutes. The number of minutes is calculated from midnight to midnight the next day. For a default day this method return
     * 24 * 60 = 1440. If on this day a summer- or wintertime switch occurs, the number of minutes are different.
     *
     * @param date the date time which includes the timezone. When setting the DateTime with the default constructor, the
     * default timezone where the application runs, is used.
     * @return the number of minutes for the day (date part) of the dateTime parameter.
     */
    public static Integer getMinutesOfDay(LocalDate date) {
        Duration duration = new Interval(date.toDateMidnight(), date.plusDays(1).toDateMidnight()).toDuration();
        return (int) duration.getStandardSeconds() / SECONDS_PER_MINUTE;
    }

    /**
     * This method gets the current DateTime, but updates the time given as a paramater.
     *
     * @param time - The time to be set.
     * @return An update LocalDateTime with the current Date and the requested time.
     */
    public static LocalDateTime getCurrentDateWithTime(LocalTime time) {
        return DateTimeUtil.getTime()
                .withTime(time.getHourOfDay(), time.getMinuteOfHour(), time.getSecondOfMinute(),
                        time.getMillisOfSecond());
    }

    /**
     * Returns the timeFactor currently being used.
     *
     * @return
     */
    public static long getTimeFactor() {
        return timeFactor;
    }

    /**
     * Generates a list with all the dates between a given interal of days.
     *
     * @param startDate {@link LocalDate} starting date of the sequence (inclusive).
     * @param endDate {@link LocalDate} ending date of the sequence (inclusive).
     * @return a {@link List} of {@link LocalDate}.
     */
    public static List<LocalDate> generateDatesOfInterval(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate period = startDate; !period.isAfter(endDate); period = period.plusDays(1)) {
            dates.add(period);
        }
        return dates;
    }

    /*
     * Calculate the actual time
     */
    private static LocalDateTime getTime() {
        if (useServer) {
            return getUDPTime();
        }
        return new LocalDateTime();
    }

    private static synchronized LocalDateTime getUDPTime() {
        do {
            if (lastProcessed == null || lastProcessed.plusMillis(BUFFER_TIME).isBefore(new LocalDateTime())) {
                String udpTime = getUDPInfo(UDP_TIME);
                if (udpTime != null) {
                    latestUDPDateTime = new LocalDateTime(udpTime);
                    lastProcessed = new LocalDateTime();
                }
            }
        } while (latestUDPDateTime == null);
        return latestUDPDateTime;
    }

    private static synchronized String getUDPInfo(String message) {
        try {
            LOGGER.debug("SENDING: {}", message);
            byte[] buf = message.getBytes();
            InetAddress address = InetAddress.getByName(serverIp);
            DatagramSocket socket = new DatagramSocket();
            socket.send(new DatagramPacket(buf, buf.length, address, port));
            DatagramPacket result = new DatagramPacket(new byte[PACKAGE_BUFFER], PACKAGE_BUFFER);
            socket.disconnect();
            socket.setSoTimeout(RESPONSE_TIMEOUT);
            socket.receive(result);
            socket.disconnect();
            socket.close();
            String resultStr = new String(result.getData()).trim();
            LOGGER.debug("RECEIVED: {} ", resultStr);
            errorCount = 0;
            return resultStr;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            errorCount++;
            if (errorCount >= MAX_ERROR_COUNT) {
                LOGGER.error("Unable to run simulation correctly.");
                System.exit(1);
            }
        }
        return null;
    }
}
