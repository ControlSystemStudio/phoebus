/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.csstudio.javafx.rtplot.internal.util.ScreenTransform;
import org.csstudio.javafx.rtplot.internal.util.TimeScreenTransform;
import org.junit.Test;

/** JUnit test of {@link TimeScreenTransform}.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimeScreenTransformTest
{
    @Test
    public void testTransform()
    {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        final ZoneId zone = ZoneId.of("America/New_York");

        final ZonedDateTime start = ZonedDateTime.of(2014, 10, 25, 13, 10, 0, 0, zone);
        assertThat(formatter.format(start), equalTo("2014-10-25 13:10:00.000"));

        final ZonedDateTime end = ZonedDateTime.of(2014, 10, 25, 13, 20, 0, 0, zone);
        assertThat(formatter.format(end), equalTo("2014-10-25 13:20:00.000"));

        final ScreenTransform<Instant> t = new TimeScreenTransform();
        // End points, time -> number
        t.config(start.toInstant(), end.toInstant(), 100.0, 200.0);
        assertThat(t.transform(start.toInstant()), equalTo(100.0));
        assertThat(t.transform(end.toInstant()), equalTo(200.0));
        // End points, number -> time
        assertThat(t.inverse(100.0), equalTo(start.toInstant()));
        assertThat(t.inverse(200.0), equalTo(end.toInstant()));
        // Mid point
        assertThat(t.transform(start.plusMinutes(5).toInstant()), equalTo(150.0));
        assertThat(formatter.format(ZonedDateTime.ofInstant(t.inverse(150.0), zone)),
                   equalTo("2014-10-25 13:15:00.000"));
    }
}
