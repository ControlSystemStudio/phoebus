/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper class to parse user defined time strings to absolute or relative
 * time durations.
 * 
 * @author shroffk
 */
public class TimeParser {

    static final Pattern lastNUnitsPattern = Pattern
            .compile("last(.*)", Pattern.CASE_INSENSITIVE);

    static final Pattern timeQunatityUnitsPattern = Pattern
            .compile("\\s*(\\d*)\\s*(ms|milli|sec|secs|min|mins|hour|hours|day|days|week|weeks)\\s*", Pattern.CASE_INSENSITIVE);

    static final Pattern nUnitsAgoPattern = Pattern
            .compile("(.*)ago", Pattern.CASE_INSENSITIVE);

    static final Pattern nUnitsAgoElementsPattern = Pattern
            .compile("\\s*(\\d*)\\s*(ms|milli|sec|secs|min|mins|hour|hours|day|days|week|weeks)\\s", Pattern.CASE_INSENSITIVE);

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

    public static TemporalAmount getDuration(String time) {
        // TODO this regular expression needs to be reviewed and improved if
        // possible

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
        Matcher timeQunatityUnitsMatcher = timeQunatityUnitsPattern.matcher(string);
        Map<ChronoUnit, Integer> timeQuantities = new HashMap<ChronoUnit, Integer>();
        while(timeQunatityUnitsMatcher.find()) {
            quantity = "".equals(timeQunatityUnitsMatcher.group(1)) ? 1 : Integer
                    .valueOf(timeQunatityUnitsMatcher.group(1));
            unit = timeQunatityUnitsMatcher.group(2).toLowerCase();
            switch (unit) {
            case "ms":
            case "milli":
                timeQuantities.put(ChronoUnit.MILLIS, quantity);
                break;
            case "sec":
            case "secs":
                timeQuantities.put(ChronoUnit.SECONDS, quantity);
                break;
            case "min":
            case "mins":
                timeQuantities.put(ChronoUnit.MINUTES, quantity);
                break;
            case "hour":
            case "hours":
                timeQuantities.put(ChronoUnit.HOURS, quantity);
                break;
            case "day":
            case "days":
                timeQuantities.put(ChronoUnit.DAYS, quantity);
                break;
            case "week":
            case "weeks":
                if (timeQuantities.containsKey(ChronoUnit.DAYS)) {
                    timeQuantities.put(ChronoUnit.DAYS, (quantity * 7) + timeQuantities.get(ChronoUnit.DAYS));
                } else {
                    timeQuantities.put(ChronoUnit.DAYS, quantity * 7);
                }
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

    public static TimeInterval getTimeInterval(String time) {
        return getTimeInterval(time, "now");
    }

    public static TimeInterval getTimeInterval(String start, String end) {
        return TimeInterval.between(getInstant(start), getInstant(end));
    }

}
