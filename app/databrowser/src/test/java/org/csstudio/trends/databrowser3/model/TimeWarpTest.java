/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;

import org.csstudio.trends.databrowser3.persistence.TimeWarp;
import org.junit.Test;

/** Test of {@link TimeWarp}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimeWarpTest
{
    @Test
    public void testGetLegacy()
    {
        assertThat(TimeWarp.formatAsLegacy(Duration.ofDays(3)), equalTo("-3 days"));
        assertThat(TimeWarp.formatAsLegacy(Duration.ofSeconds(30)), equalTo("-30 seconds"));
        assertThat(TimeWarp.formatAsLegacy(Duration.ofHours(2)), equalTo("-2 hours"));
        assertThat(TimeWarp.formatAsLegacy(Duration.ofSeconds(120)), equalTo("-2 minutes"));
        assertThat(TimeWarp.formatAsLegacy(Duration.ofDays(3).plus(Duration.ofSeconds(10, 123000000))), equalTo("-3 days -10.123 seconds"));
    }

    @Test
    public void testParseLegacy()
    {
        TemporalAmount amount = TimeWarp.parseLegacy("-3 days");
        assertThat(amount, equalTo(Duration.ofDays(3)));

        amount = TimeWarp.parseLegacy("-30.00 days");
        assertThat(amount, equalTo(Duration.ofDays(30)));

        amount = TimeWarp.parseLegacy("-3 months");
        assertThat(amount, equalTo(Period.ofMonths(3)));

        amount = TimeWarp.parseLegacy("-3 days -10.123 seconds");
        assertThat(amount, equalTo(Duration.ofSeconds(3*24*60*60 + 10, 123000000)));

        amount = TimeWarp.parseLegacy("  -2.00 hours    ");
        assertThat(amount, equalTo(Duration.ofHours(2)));

        amount = TimeWarp.parseLegacy(" -1 day -2.00 hours -3 minutes   ");
        assertThat(amount, equalTo(Duration.ofMinutes(24L*60 + 2*60 + 3)));

        amount = TimeWarp.parseLegacy("-2.00 h");
        assertThat(amount, equalTo(Duration.ofHours(2)));

        amount = TimeWarp.parseLegacy("-1.95 min");
        assertThat(amount, equalTo(Duration.ofSeconds(117)));
    }
}
