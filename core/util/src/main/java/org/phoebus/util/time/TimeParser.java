/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.time.temporal.ChronoUnit.*;

/**
 * A helper class to parse user defined time strings to absolute or relative
 * time durations.
 * 
 * @author shroffk
 */
public class TimeParser {
    
    static final Pattern durationTimeQunatityUnitsPattern = Pattern
            .compile("\\s*(\\d*)\\s*(ms|milli|sec|secs|min|mins|hour|hours|day|days)\\s*", Pattern.CASE_INSENSITIVE);
    
    static final Pattern timeQunatityUnitsPattern = Pattern.compile(
            "\\s*(\\d*)\\s*(ms|milli|s|sec|secs|min|mins|h|hour|hours|d|day|days|w|week|weeks|month|months|y|year|years)\\s*",
            Pattern.CASE_INSENSITIVE);

    /**
     * A Helper function to help you convert various string represented time
     * definitions to an absolute Instant.
     *
     * @param time a string that represents an instant in time
     * @return the parsed Instant or null
     */
    public static Instant getInstant(String time) {
        if (time.equalsIgnoreCase("now")) {
            return Instant.now();
        } else {
            Matcher nUnitsAgoMatcher = timeQunatityUnitsPattern.matcher(time);
            while (nUnitsAgoMatcher.find()) {
                return Instant.now().minus(parseDuration(nUnitsAgoMatcher.group(1)));
            }
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            return LocalDateTime.parse(time, formatter).atZone(TimeZone.getDefault().toZoneId()).toInstant();
        }
    }

    /**
     * Return a {@link TimeInterval} between this instant represented by the string and "now"
     * @param time
     * @return TimeInterval
     */
    public static TimeInterval getTimeInterval(String time) {
        Instant now = Instant.now();
        if (time.equalsIgnoreCase("now")) {
            return TimeInterval.between(now, now);
        } else {
            Matcher nUnitsAgoMatcher = timeQunatityUnitsPattern.matcher(time);
            while (nUnitsAgoMatcher.find()) {
                return TimeInterval.between(now.minus(parseTemporalAmount(nUnitsAgoMatcher.group(1))), now);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            return TimeInterval.between(LocalDateTime.parse(time, formatter).atZone(TimeZone.getDefault().toZoneId()).toInstant(), now);
        }
    }


    private final static List<ChronoUnit> durationUnits = Arrays.asList(MILLIS, SECONDS, MINUTES, HOURS);

    /**
     * parses the given string into a {@link Duration}. The method only supports
     * {@link ChronoUnit#MILLIS}, {@link ChronoUnit#SECONDS},
     * {@link ChronoUnit#MINUTES}, and {@link ChronoUnit#HOURS}. Days {@link ChronoUnit#DAYS} are treated as 24 HOURS.
     * 
     * e.g. parseDuraiton("5h 3min 34s");
     * 
     * @param string
     * @return
     */
    public static Duration parseDuration(String string) {
        int quantity = 0;
        String unit = "";
        Matcher timeQunatityUnitsMatcher = durationTimeQunatityUnitsPattern.matcher(string);
        Map<ChronoUnit, Integer> timeQuantities = new HashMap<ChronoUnit, Integer>();
        while (timeQunatityUnitsMatcher.find()) {
            quantity = "".equals(timeQunatityUnitsMatcher.group(1)) ? 1
                    : Integer.valueOf(timeQunatityUnitsMatcher.group(1));
            unit = timeQunatityUnitsMatcher.group(2).toLowerCase();
            switch (unit) {
            case "ms":
            case "milli":
                timeQuantities.put(MILLIS, quantity);
                break;
            case "s":
            case "sec":
            case "secs":
                timeQuantities.put(SECONDS, quantity);
                break;
            case "m":
            case "min":
            case "mins":
                timeQuantities.put(MINUTES, quantity);
                break;
            case "h":
            case "hour":
            case "hours":
                timeQuantities.put(HOURS, quantity);
                break;
            case "d":
            case "day":
            case "days":
                timeQuantities.put(DAYS, quantity);
                break;
            default:
                break;
            }
        }
        Duration duration = Duration.ofSeconds(0);
        for (Entry<ChronoUnit, Integer> entry : timeQuantities.entrySet()) {
            duration = duration.plus(entry.getValue(), entry.getKey());
        }
        return duration;
    }

    /**
     * 
     * @param string
     * @return
     */
    public static TemporalAmount parseTemporalAmount(String string) {
        int quantity = 0;
        String unit = "";
        Matcher timeQunatityUnitsMatcher = timeQunatityUnitsPattern.matcher(string);
        Map<ChronoUnit, Integer> timeQuantities = new HashMap<ChronoUnit, Integer>();
        while (timeQunatityUnitsMatcher.find()) {
            quantity = "".equals(timeQunatityUnitsMatcher.group(1)) ? 1
                    : Integer.valueOf(timeQunatityUnitsMatcher.group(1));
            unit = timeQunatityUnitsMatcher.group(2).toLowerCase();
            switch (unit) {
            case "ms":
            case "milli":
                timeQuantities.put(MILLIS, quantity);
                break;
            case "sec":
            case "secs":
                timeQuantities.put(SECONDS, quantity);
                break;
            case "min":
            case "mins":
                timeQuantities.put(MINUTES, quantity);
                break;
            case "hour":
            case "hours":
                timeQuantities.put(HOURS, quantity);
                break;
            case "day":
            case "days":
                timeQuantities.put(DAYS, quantity);
                break;
            case "w":
            case "week":
            case "weeks":
                timeQuantities.put(WEEKS, quantity);
                break;
            case "month":
            case "months":
                timeQuantities.put(MONTHS, quantity);
                break;
            case "y":
            case "year":
            case "years":
                timeQuantities.put(YEARS, quantity);
                break;
            default:
                break;
            }
        }
        if (Collections.disjoint(timeQuantities.keySet(), durationUnits)) {
            Period result = Period.ZERO;
            result = result.plusYears(timeQuantities.containsKey(YEARS) ? timeQuantities.get(YEARS) : 0);
            result = result.plusMonths(timeQuantities.containsKey(MONTHS) ? timeQuantities.get(MONTHS) : 0);
            result = result.plusDays(timeQuantities.containsKey(DAYS) ? timeQuantities.get(DAYS) : 0);
            return result;
        } else {
            Duration result = Duration.ofSeconds(0);
            for (Entry<ChronoUnit, Integer> entry : timeQuantities.entrySet()) {
                result = result.plus(entry.getValue(), entry.getKey());
            }
            return result;
        }
    }

}
