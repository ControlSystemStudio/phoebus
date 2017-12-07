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

    static final Pattern lastNUnitsPattern = Pattern
            .compile("last(.*)", Pattern.CASE_INSENSITIVE);

    static final Pattern nUnitsAgoPattern = Pattern
            .compile("(.*)ago", Pattern.CASE_INSENSITIVE);

    static final Pattern durationTimeQunatityUnitsPattern = Pattern
            .compile("\\s*(\\d*)\\s*(ms|milli|sec|secs|min|mins|hour|hours|day|days)\\s*", Pattern.CASE_INSENSITIVE);
    

    static final Pattern timeQunatityUnitsPattern = Pattern
            .compile("\\s*(\\d*)\\s*(ms|milli|sec|secs|min|mins|hour|hours|day|days|month|months|year|years)\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * A Helper function to help you convert various string represented time
     * definition to an absolute Instant.
     *
     * @param time a string that represents an instant in time
     * @return the parsed Instant or null
     */
    public static Instant getInstant(String time) {
        if (time.equalsIgnoreCase("now")) {
            return Instant.now();
        } else {
            Matcher lastNUnitsMatcher = lastNUnitsPattern.matcher(time);
            while (lastNUnitsMatcher.find()) {
                return Instant.now().minus(parseDuration(lastNUnitsMatcher.group(1)));
            }
            Matcher nUnitsAgoMatcher = nUnitsAgoPattern.matcher(time);
            while (nUnitsAgoMatcher.find()) {
                return Instant.now().minus(parseDuration(nUnitsAgoMatcher.group(1)));
            }
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            return LocalDateTime.parse(time, formatter).atZone(TimeZone.getDefault().toZoneId()).toInstant();
        }
    }

    /**
     * 
     * @param time a string description of a time duration
     * @return Duration from the parsed string
     */
    public static Duration getDuration(String time) {

        Matcher lastNUnitsMatcher = lastNUnitsPattern.matcher(time);
        if(lastNUnitsMatcher.find()) {
            return parseDuration(lastNUnitsMatcher.group(1));
        }
        Matcher nUnitsAgoMatcher = nUnitsAgoPattern.matcher(time);
        if(nUnitsAgoMatcher.find()) {
            return parseDuration(nUnitsAgoMatcher.group(1));
        }
        return null;
    }

    private static Duration parseDuration(String string) {
        int quantity = 0;
        String unit = "";
        Matcher timeQunatityUnitsMatcher = durationTimeQunatityUnitsPattern.matcher(string);
        Map<ChronoUnit, Integer> timeQuantities = new HashMap<ChronoUnit, Integer>();
        while(timeQunatityUnitsMatcher.find()) {
            quantity = "".equals(timeQunatityUnitsMatcher.group(1)) ? 1 : Integer
                    .valueOf(timeQunatityUnitsMatcher.group(1));
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
     * @param time
     * @return TimeInterval
     */
    public static TimeInterval getTimeInterval(String time) {
        return getTimeInterval(time, "now");
    }

    /**
     * 
     * @param start
     * @param end
     * @return TimeInterval
     */
    public static TimeInterval getTimeInterval(String start, String end) {
        return TimeInterval.between(getInstant(start), getInstant(end));
    }

    /**
     * Parses the string trying its best to handle both calendar aware {@link Period} and calendar agnostic {@link Duration} 
     * @param start
     * @param end
     * @return TimeRelativeInterval
     */
    public static TimeRelativeInterval getTimeRelativeInterval(String start, String end) {
        return TimeRelativeInterval.of(parseTimeRelativeInterval(start), parseTimeRelativeInterval(end));
    }

    private final static List<ChronoUnit> durationUnits = Arrays.asList(MILLIS, SECONDS, MINUTES, HOURS);

    private static TemporalAmount parseTimeRelativeInterval(String string) {
        int quantity = 0;
        String unit = "";
        Matcher timeQunatityUnitsMatcher = timeQunatityUnitsPattern.matcher(string);
        Map<ChronoUnit, Integer> timeQuantities = new HashMap<ChronoUnit, Integer>();
        while(timeQunatityUnitsMatcher.find()) {
            quantity = "".equals(timeQunatityUnitsMatcher.group(1)) ? 1 : Integer
                    .valueOf(timeQunatityUnitsMatcher.group(1));
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
            case "month":
            case "months":
                timeQuantities.put(MONTHS, quantity);
                break;
            case "year":
            case "years":
                timeQuantities.put(YEARS, quantity);
                break;
            default:
                break;
            }
        }
        if(Collections.disjoint(timeQuantities.keySet(), durationUnits)) {
            Period result = Period.ZERO;
            result = result.plusYears(timeQuantities.containsKey(YEARS)?timeQuantities.get(YEARS):0);
            result = result.plusMonths(timeQuantities.containsKey(MONTHS)?timeQuantities.get(MONTHS):0);
            result = result.plusDays(timeQuantities.containsKey(DAYS)?timeQuantities.get(DAYS):0);
            return result;
        }
        else {
            Duration result = Duration.ofSeconds(0);
            for (Entry<ChronoUnit, Integer> entry : timeQuantities.entrySet()) {
                result = result.plus(entry.getValue(), entry.getKey());
            }
            return result;
        }
    }

}
