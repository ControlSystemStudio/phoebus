/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.Instant;

import org.junit.Test;
import org.phoebus.util.time.TimeDuration;
import org.phoebus.util.time.TimestampFormats;

/** JUnit test of {@link TimestampHelper}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimestampHelperTest
{
    @Test
    public void testRoundUp() throws Exception
    {
        final Instant orig = Instant.from(TimestampFormats.SECONDS_FORMAT.parse("2012-01-19 12:23:14"));
        String text = TimestampFormats.SECONDS_FORMAT.format(orig);
        System.out.println(text);
        assertThat(text, equalTo("2012-01-19 12:23:14"));

        Instant time;

        // Round within a few seconds
        time = TimestampHelper.roundUp(orig, 10);
        text = TimestampFormats.SECONDS_FORMAT.format(time);
        System.out.println(text);
        assertThat(text, equalTo("2012-01-19 12:23:20"));

        time = TimestampHelper.roundUp(orig, TimeDuration.ofSeconds(30));
        text = TimestampFormats.SECONDS_FORMAT.format(time);
        System.out.println(text);
        assertThat(text, equalTo("2012-01-19 12:23:30"));

        // .. to minute
        time = TimestampHelper.roundUp(orig, 60);
        text = TimestampFormats.SECONDS_FORMAT.format(time);
        System.out.println(text);
        assertThat(text, equalTo("2012-01-19 12:24:00"));

        // .. to hours
        time = TimestampHelper.roundUp(orig, TimeDuration.ofHours(1.0));
        text = TimestampFormats.SECONDS_FORMAT.format(time);
        System.out.println(text);
        assertThat(text, equalTo("2012-01-19 13:00:00"));

        time = TimestampHelper.roundUp(orig, 2L*60*60);
        text = TimestampFormats.SECONDS_FORMAT.format(time);
        System.out.println(text);
        assertThat(text, equalTo("2012-01-19 14:00:00"));

        // .. full day(s)
        assertThat(24L*60*60, equalTo(TimestampHelper.SECS_PER_DAY));

        time = TimestampHelper.roundUp(orig, TimestampHelper.SECS_PER_DAY);
        text = TimestampFormats.SECONDS_FORMAT.format(time);
        System.out.println(text);
        assertThat(text, equalTo("2012-01-20 00:00:00"));

        time = TimestampHelper.roundUp(orig, 3*TimestampHelper.SECS_PER_DAY);
        text = TimestampFormats.SECONDS_FORMAT.format(time);
        System.out.println(text);
        assertThat(text, equalTo("2012-01-22 00:00:00"));

        // Into next month
        time = TimestampHelper.roundUp(orig, 13*TimestampHelper.SECS_PER_DAY);
        text = TimestampFormats.SECONDS_FORMAT.format(time);
        System.out.println(text);
        assertThat(text, equalTo("2012-02-01 00:00:00"));

        // .. full day(s)
        assertThat(24L*60*60, equalTo(TimestampHelper.SECS_PER_DAY));

        // 1.5 days
        time = TimestampHelper.roundUp(orig, (3*TimestampHelper.SECS_PER_DAY)/2);
        text = TimestampFormats.SECONDS_FORMAT.format(time);
        System.out.println(text);
        assertThat(text, equalTo("2012-01-20 12:00:00"));
    }
}
