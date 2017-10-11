/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import static java.time.Duration.between;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeParser;
/**
 *
 * @author shroffk
 */
public class TimeParserTest {

    @Test
    public void getNow() {
        Instant ts = TimeParser.getInstant("now");
        Assert.assertTrue("Failed to obtain Timestamp corresponding to now ",
                ts != null && ts instanceof Instant);
    }

    /**
     * Test the times Duration ← relative
     */
    @Test
    public void getDuration() {
        // "last min", "last hour", "last day", "last week"
        Duration lastMin = TimeParser.getDuration("last min");
        Assert.assertEquals("Failed to get Duration for last min", 60,
                lastMin.getSeconds());
        Duration lastHour = TimeParser.getDuration("last hour");
        Assert.assertEquals("Failed to get Duration for last hour",
                60 * 60, lastHour.getSeconds());
        Duration lastDay = TimeParser.getDuration("last day");
        Assert.assertEquals("Failed to get Duration for last day",
                60 * 60 * 24, lastDay.getSeconds());
        Duration lastWeek = TimeParser.getDuration("last week");
        Assert.assertEquals("Failed to get Duration for last week",
                60 * 60 * 24 * 7, lastWeek.getSeconds());

        // "last 5 mins", "last 5 hours", "last 5 days", "last 5 weeks"
        Duration last5Min = TimeParser.getDuration("last 5 mins");
        Assert.assertEquals("Failed to get Duration for last 5 mins",
                60 * 5, last5Min.getSeconds());
        Duration last5Hour = TimeParser.getDuration("last 5 hours");
        Assert.assertEquals("Failed to get Duration for last 5 hours",
                60 * 60 * 5, last5Hour.getSeconds());
        Duration last5Day = TimeParser.getDuration("last 5 days");
        Assert.assertEquals("Failed to get Duration for last 5 days",
                60 * 60 * 24 * 5, last5Day.getSeconds());
        Duration last5Week = TimeParser.getDuration("last 5 weeks");
        Assert.assertEquals("Failed to get Duration for last 5 weeks", 60
                * 60 * 24 * 7 * 5, last5Week.getSeconds());

        // "1 min ago", "1 hours ago", "1 days ago", "1 weeks ago"
        Duration oneMinAgo = TimeParser.getDuration("1 min ago");
        Assert.assertEquals("Failed to get Duration for 1 min ago", 60,
                oneMinAgo.getSeconds());
        Duration oneHourAgo = TimeParser.getDuration("1 hour ago");
        Assert.assertEquals("Failed to get Duration for 1 hour ago",
                60 * 60, oneHourAgo.getSeconds());
        Duration oneDayAgo = TimeParser.getDuration("1 day ago");
        Assert.assertEquals("Failed to get Duration for 1 days ago",
                60 * 60 * 24, oneDayAgo.getSeconds());
        Duration oneWeekAgo = TimeParser.getDuration("1 week ago");
        Assert.assertEquals("Failed to get Duration for 1 week ago",
                60 * 60 * 24 * 7, oneWeekAgo.getSeconds());

        // "5 mins ago", "5 hours ago", "5 days ago", "5 weeks ago"
        Duration fiveMinsAgo = TimeParser.getDuration("5 mins ago");
        Assert.assertEquals("Failed to get Duration for 5 mins ago",
                60 * 5, fiveMinsAgo.getSeconds());
        Duration fiveHoursAgo = TimeParser.getDuration("5 hours ago");
        Assert.assertEquals("Failed to get Duration for 5 hours ago",
                60 * 60 * 5, fiveHoursAgo.getSeconds());
        Duration fiveDaysAgo = TimeParser.getDuration("5 days ago");
        Assert.assertEquals("Failed to get Duration for 5 days ago",
                60 * 60 * 24 * 5, fiveDaysAgo.getSeconds());
        Duration fiveWeeksAgo = TimeParser.getDuration("5 weeks ago");
        Assert.assertEquals("Failed to get Duration for 5 week ago", 60
                * 60 * 24 * 7 * 5, fiveWeeksAgo.getSeconds());

        // Check case insensitivity Last 4 Mins, Last 4 Hours, Last 4 Days, Last
        // 4 WEEKS
        Duration last4Min = TimeParser.getDuration("Last 4 Mins");
        Assert.assertEquals("Failed to get Duration for Last 4 Mins",
                60 * 4, last4Min.getSeconds());
        Duration last4Hour = TimeParser.getDuration("Last 4 Hours");
        Assert.assertEquals("Failed to get Duration for Last 4 Hours",
                60 * 60 * 4, last4Hour.getSeconds());
        Duration last4Day = TimeParser.getDuration("Last 4 Day");
        Assert.assertEquals("Failed to get Duration for Last 4 Day",
                60 * 60 * 24 * 4, last4Day.getSeconds());
        Duration last4Week = TimeParser.getDuration("Last 4 WEEKS");
        Assert.assertEquals("Failed to get Duration for Last 4 WEEKS", 60
                * 60 * 24 * 7 * 4, last4Week.getSeconds());

        // Check incorrect units in terms of plurality last 3 min, last 3 hour,
        // last 3 day, last 3 week
        Duration last3Min = TimeParser.getDuration("last 3 min");
        Assert.assertEquals("Failed to get Duration for last 3 min",
                60 * 3, last3Min.getSeconds());
        Duration last3Hour = TimeParser.getDuration("last 3 hour");
        Assert.assertEquals("Failed to get Duration for last 3 hour",
                60 * 60 * 3, last3Hour.getSeconds());
        Duration last3Day = TimeParser.getDuration("last 3 day");
        Assert.assertEquals("Failed to get Duration for last 3 day",
                60 * 60 * 24 * 3, last3Day.getSeconds());
        Duration last3Week = TimeParser.getDuration("last 3 week");
        Assert.assertEquals("Failed to get Duration for last 3 week", 60
                * 60 * 24 * 7 * 3, last3Week.getSeconds());

        // Check missing space between time quantity and unit last 2mins, last
        // 2hours, last 2days, last 2weeks, 2mins ago, 2hours ago, 2days ago,
        // 2weeks ago
        Duration last2Mins = TimeParser.getDuration("last 2mins");
        Assert.assertEquals("Failed to get Duration for last 2mins",
                60 * 2, last2Mins.getSeconds());
        Duration last2Hours = TimeParser.getDuration("last 2hours");
        Assert.assertEquals("Failed to get Duration for last 2hours",
                60 * 60 * 2, last2Hours.getSeconds());
        Duration last2Days = TimeParser.getDuration("last 2days");
        Assert.assertEquals("Failed to get Duration for last 2days",
                60 * 60 * 24 * 2, last2Days.getSeconds());
        Duration last2Weeks = TimeParser.getDuration("last 2weeks");
        Assert.assertEquals("Failed to get Duration for last 2weeks", 60
                * 60 * 24 * 7 * 2, last2Weeks.getSeconds());
        Duration twoMinsAgo = TimeParser.getDuration("2mins ago");
        Assert.assertEquals("Failed to get Duration for 2mins ago", 60 * 2,
                twoMinsAgo.getSeconds());
        Duration twoHoursAgo = TimeParser.getDuration("2hours ago");
        Assert.assertEquals("Failed to get Duration for 2hours ago",
                60 * 60 * 2, twoHoursAgo.getSeconds());
        Duration twoDaysAgo = TimeParser.getDuration("2days ago");
        Assert.assertEquals("Failed to get Duration for 2days ago",
                60 * 60 * 24 * 2, twoDaysAgo.getSeconds());
        Duration twoWeeksAgo = TimeParser.getDuration("2weeks ago");
        Assert.assertEquals("Failed to get Duration for 2weeks ago", 60
                * 60 * 24 * 7 * 2, twoWeeksAgo.getSeconds());

    }

    /**
     * Test the times TimeInterval ← relative
     */
    @Test
    public void getTimeInterval() {
        // "last min", "last hour", "last day", "last week"
        TimeInterval lastMin = TimeParser.getTimeInterval("last min");
        Assert.assertEquals("Failed to get TimeInterval for last min", 60,
                between(lastMin.getStart(), lastMin.getEnd()).getSeconds());
        TimeInterval lastHour = TimeParser.getTimeInterval("last hour");
        Assert.assertEquals("Failed to get TimeInterval for last hour",
                (60 * 60),
                between(lastHour.getStart(), lastHour.getEnd()).getSeconds(), 0);
        TimeInterval lastDay = TimeParser.getTimeInterval("last day");
        Assert.assertEquals(
                "Failed to get TimeInterval for last day",
                (60 * 60 * 24),
                between(lastDay.getStart(), lastDay.getEnd()).getSeconds(),
                0);
        TimeInterval lastWeek = TimeParser.getTimeInterval("last week");
        Assert.assertEquals("Failed to get TimeInterval for last week",
                (60 * 60 * 24 * 7),
                between(lastWeek.getStart(), lastWeek.getEnd())
                        .getSeconds(), 0);

        // "last 5 mins", "last 5 hours", "last 5 days", "last 5 weeks"
        TimeInterval last5Min = TimeParser.getTimeInterval("last 5 mins");
        Assert.assertEquals("Failed to get TimeInterval for last 5 mins",
                (60 * 5),
                between(last5Min.getStart(), last5Min.getEnd())
                        .getSeconds(), 0);
        TimeInterval last5Hour = TimeParser.getTimeInterval("last 5 hours");
        Assert.assertEquals("Failed to get TimeInterval for last 5 hours",
                (60 * 60 * 5),
                between(last5Hour.getStart(), last5Hour.getEnd())
                        .getSeconds(), 0);
        TimeInterval last5Day = TimeParser.getTimeInterval("last 5 days");
        Assert.assertEquals("Failed to get TimeInterval for last 5 days",
                (60 * 60 * 24 * 5),
                between(last5Day.getStart(), last5Day.getEnd())
                        .getSeconds(), 0);
        TimeInterval last5Week = TimeParser.getTimeInterval("last 5 weeks");
        Assert.assertEquals("Failed to get TimeInterval for last 5 weeks",
                (60 * 60 * 24 * 7 * 5),
                between(last5Week.getStart(), last5Week.getEnd())
                        .getSeconds(), 0);

        // "1 min ago", "1 hours ago", "1 days ago", "1 weeks ago"
        TimeInterval oneMinAgo = TimeParser.getTimeInterval("1 min ago");
        Assert.assertEquals("Failed to get TimeInterval for 1 min ago", (60),
                between(oneMinAgo.getStart(), oneMinAgo.getEnd())
                        .getSeconds(), 0);
        TimeInterval oneHourAgo = TimeParser.getTimeInterval("1 hour ago");
        Assert.assertEquals("Failed to get TimeInterval for 1 hour ago",
                (60 * 60),
                between(oneHourAgo.getStart(), oneHourAgo.getEnd())
                        .getSeconds(), 0);
        TimeInterval oneDayAgo = TimeParser.getTimeInterval("1 day ago");
        Assert.assertEquals("Failed to get TimeInterval for 1 days ago",
                (60 * 60 * 24),
                between(oneDayAgo.getStart(), oneDayAgo.getEnd())
                        .getSeconds(), 0);
        TimeInterval oneWeekAgo = TimeParser.getTimeInterval("1 week ago");
        Assert.assertEquals("Failed to get TimeInterval for 1 week ago",
                (60 * 60 * 24 * 7),
                between(oneWeekAgo.getStart(), oneWeekAgo.getEnd())
                        .getSeconds(), 0);

        // "5 mins ago", "5 hours ago", "5 days ago", "5 weeks ago"
        TimeInterval fiveMinsAgo = TimeParser.getTimeInterval("5 mins ago");
        Assert.assertEquals("Failed to get TimeInterval for 5 mins ago",
                (60 * 5),
                between(fiveMinsAgo.getStart(), fiveMinsAgo.getEnd())
                        .getSeconds(), 0);
        TimeInterval fiveHoursAgo = TimeParser.getTimeInterval("5 hours ago");
        Assert.assertEquals("Failed to get TimeInterval for 5 hours ago",
                (60 * 60 * 5),
                between(fiveHoursAgo.getStart(), fiveHoursAgo.getEnd())
                        .getSeconds(), 0);
        TimeInterval fiveDaysAgo = TimeParser.getTimeInterval("5 days ago");
        Assert.assertEquals("Failed to get TimeInterval for 5 days ago",
                (60 * 60 * 24 * 5),
                between(fiveDaysAgo.getStart(), fiveDaysAgo.getEnd())
                        .getSeconds(), 0);
        TimeInterval fiveWeeksAgo = TimeParser.getTimeInterval("5 weeks ago");
        Assert.assertEquals("Failed to get TimeInterval for 5 week ago",
                (60 * 60 * 24 * 7 * 5), between(fiveWeeksAgo.getStart()
                        , fiveWeeksAgo.getEnd()).getSeconds(), 0);

        // Check case insensitivity Last 4 Mins, Last 4 Hours, Last 4 Days, Last
        // 4 WEEKS
        TimeInterval last4Min = TimeParser.getTimeInterval("Last 4 Mins");
        Assert.assertEquals("Failed to get TimeInterval for Last 4 Mins",
                (60 * 4),
                between(last4Min.getStart(), last4Min.getEnd())
                        .getSeconds(), 0);
        TimeInterval last4Hour = TimeParser.getTimeInterval("Last 4 Hours");
        Assert.assertEquals("Failed to get TimeInterval for Last 4 Hours",
                (60 * 60 * 4),
                between(last4Hour.getStart(), last4Hour.getEnd())
                        .getSeconds(), 0);
        TimeInterval last4Day = TimeParser.getTimeInterval("Last 4 Day");
        Assert.assertEquals("Failed to get TimeInterval for Last 4 Day",
                (60 * 60 * 24 * 4),
                between(last4Day.getStart(), last4Day.getEnd())
                        .getSeconds(), 0);
        TimeInterval last4Week = TimeParser.getTimeInterval("Last 4 WEEKS");
        Assert.assertEquals("Failed to get TimeInterval for Last 4 WEEKS",
                (60 * 60 * 24 * 7 * 4),
                between(last4Week.getStart(), last4Week.getEnd())
                        .getSeconds(), 0);

        // Check incorrect units in terms of plurality last 3 min, last 3 hour,
        // last 3 day, last 3 week
        TimeInterval last3Min = TimeParser.getTimeInterval("last 3 min");
        Assert.assertEquals("Failed to get TimeInterval for last 3 min",
                (60 * 3),
                between(last3Min.getStart(), last3Min.getEnd())
                        .getSeconds(), 0);
        TimeInterval last3Hour = TimeParser.getTimeInterval("last 3 hour");
        Assert.assertEquals("Failed to get TimeInterval for last 3 hour",
                (60 * 60 * 3),
                between(last3Hour.getStart(), last3Hour.getEnd())
                        .getSeconds(), 0);
        TimeInterval last3Day = TimeParser.getTimeInterval("last 3 day");
        Assert.assertEquals("Failed to get TimeInterval for last 3 day",
                (60 * 60 * 24 * 3),
                between(last3Day.getStart(), last3Day.getEnd())
                        .getSeconds(), 0);
        TimeInterval last3Week = TimeParser.getTimeInterval("last 3 week");
        Assert.assertEquals("Failed to get TimeInterval for last 3 week",
                (60 * 60 * 24 * 7 * 3),
                between(last3Week.getStart(), last3Week.getEnd())
                        .getSeconds(), 0);

        // Check missing space between time quantity and unit last 2mins, last
        // 2hours, last 2days, last 2weeks, 2mins ago, 2hours ago, 2days ago,
        // 2weeks ago
        TimeInterval last2Mins = TimeParser.getTimeInterval("last 2mins");
        Assert.assertEquals("Failed to get TimeInterval for last 2mins",
                (60 * 2),
                between(last2Mins.getStart(), last2Mins.getEnd())
                        .getSeconds(), 0);
        TimeInterval last2Hours = TimeParser.getTimeInterval("last 2hours");
        Assert.assertEquals("Failed to get TimeInterval for last 2hours",
                (60 * 60 * 2),
                between(last2Hours.getStart(), last2Hours.getEnd())
                        .getSeconds(), 0);
        TimeInterval last2Days = TimeParser.getTimeInterval("last 2days");
        Assert.assertEquals("Failed to get TimeInterval for last 2days",
                (60 * 60 * 24 * 2),
                between(last2Days.getStart(), last2Days.getEnd())
                        .getSeconds(), 0);
        TimeInterval last2Weeks = TimeParser.getTimeInterval("last 2weeks");
        Assert.assertEquals("Failed to get TimeInterval for last 2weeks",
                (60 * 60 * 24 * 7 * 2),
                between(last2Weeks.getStart(), last2Weeks.getEnd())
                        .getSeconds(), 0);
        TimeInterval twoMinsAgo = TimeParser.getTimeInterval("2mins ago");
        Assert.assertEquals("Failed to get TimeInterval for 2mins ago",
                (60 * 2),
                between(twoMinsAgo.getStart(), twoMinsAgo.getEnd())
                        .getSeconds(), 0);
        TimeInterval twoHoursAgo = TimeParser.getTimeInterval("2hours ago");
        Assert.assertEquals("Failed to get TimeInterval for 2hours ago",
                (60 * 60 * 2),
                between(twoHoursAgo.getStart(), twoHoursAgo.getEnd())
                        .getSeconds(), 0);
        TimeInterval twoDaysAgo = TimeParser.getTimeInterval("2days ago");
        Assert.assertEquals("Failed to get TimeInterval for 2days ago",
                (60 * 60 * 24 * 2),
                between(twoDaysAgo.getStart(), twoDaysAgo.getEnd())
                        .getSeconds(), 0);
        TimeInterval twoWeeksAgo = TimeParser.getTimeInterval("2weeks ago");
        Assert.assertEquals("Failed to get TimeInterval for 2weeks ago",
                (60 * 60 * 24 * 7 * 2),
                between(twoWeeksAgo.getStart(), twoWeeksAgo.getEnd()).getSeconds(), 0);
    }

    /**
     * Test the creation of time intervals using from: and to:
     */
    @Test
    public void getAbsoluteTimeInterval() {
        TimeInterval oneMin = TimeParser.getTimeInterval("3 mins ago",
                "2 mins ago");
        Assert.assertEquals(
                "Failed to get time Interval for String: from:3 mins ago to:2 mins ago",
                60,
                between(oneMin.getStart(), oneMin.getEnd()).getSeconds(), 0);
        // Explicitly define the time
        String startTime = "1976-01-01T00:00:00";
        String endTime = "1976-01-02T00:00:00";
        TimeInterval oneDay = TimeParser.getTimeInterval(startTime, endTime);
        Assert.assertEquals("Failed to get time Interval for String: from:"
                + startTime + " to:" + endTime, 60 * 60 * 24,
                between(oneDay.getStart(), oneDay.getEnd()).getSeconds(), 0);

    }
}
