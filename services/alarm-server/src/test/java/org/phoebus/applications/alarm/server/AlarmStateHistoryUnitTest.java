/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;
import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.SeverityLevel;

/** JUnit test of the {@link AlarmStateHistory}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmStateHistoryUnitTest
{
    @Test
    public void testAlarmStateHistory()
    {
        // Check for 2 faults within 3 seconds
        final AlarmStateHistory history = new AlarmStateHistory(2);
        assertThat(history.receivedAlarmsWithinTimerange(3.0), equalTo(false));

        // 1 fault at the 'start' time
        final Instant start = Instant.now();
        history.add(new AlarmState(SeverityLevel.MINOR, "Low", "1", start));
        assertThat(history.receivedAlarmsWithinTimerange(3.0), equalTo(false));

        // 2 faults within _4_ seconds
        history.add(new AlarmState(SeverityLevel.MINOR, "Low", "1", start.plus(Duration.ofSeconds(4))));
        assertThat(history.receivedAlarmsWithinTimerange(3.0), equalTo(false));

        // Original fault drops off the list. Keeping 2 faults within _2_ seconds
        history.add(new AlarmState(SeverityLevel.MINOR, "Low", "1", start.plus(Duration.ofSeconds(6))));
        assertThat(history.receivedAlarmsWithinTimerange(3.0), equalTo(true));
    }
}
