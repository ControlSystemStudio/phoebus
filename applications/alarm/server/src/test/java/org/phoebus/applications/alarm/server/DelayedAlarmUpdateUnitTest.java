/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.Date;

import org.junit.Test;
import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.SeverityLevel;

/** JUnit test of the {@link DelayedAlarmUpdate}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DelayedAlarmUpdateUnitTest
{
    private AlarmState delayed_state = null;

    public void delayedStateUpdate(AlarmState delayed_state)
    {
        System.out.println(new Date() + ": Received update!");
        synchronized (this)
        {
            this.delayed_state = delayed_state;
            notifyAll();
        }
    }

    @Test
    public void testDelayedAlarmUpdate() throws Exception
    {
        final DelayedAlarmUpdate delay = new DelayedAlarmUpdate(this::delayedStateUpdate);
        final AlarmState state = new AlarmState(SeverityLevel.MAJOR, "Test", null, Instant.now());
        System.out.println(new Date() + ": Scheduling delayed alarm update ...");
        delay.schedule_update(state, 2);

        assertEquals(state, delay.getState());
        // Expect nothing right away
        synchronized (this)
        {
            assertNull(delayed_state);
        }

        // .. but after ~2 seconds, the update should arrive
        synchronized (this)
        {
            for (int secs=0; delayed_state == null  &&  secs<4; ++secs)
                wait(1000);
            assertEquals(state, delayed_state);
        }

        // Delay should be 'idle', no pending state
        assertNull(delay.getState());
    }


    @Test
    public void testCancellation() throws Exception
    {
        final DelayedAlarmUpdate delay = new DelayedAlarmUpdate(this::delayedStateUpdate);
        final AlarmState state = new AlarmState(SeverityLevel.MAJOR, "Test", null, Instant.now());
        delay.schedule_update(state, 2);

        assertEquals(state, delay.getState());
        // Expect nothing right away
        synchronized (this)
        {
            assertNull(delayed_state);
        }
        // Cancel
        delay.cancel();
        delay.cancel();
        delay.cancel();

        // Even after ~2 seconds, no update should arrive
        Thread.sleep(2000);
        synchronized (this)
        {
            assertNull(delayed_state);
        }
        // Delay should be 'idle', no pending state
        assertNull(delay.getState());
    }

    /** Test if delayed update stops completely when one of its updates crashes */
    @Test
    public void testCrashInTimer() throws Exception
    {
        final DelayedAlarmUpdate delay = new DelayedAlarmUpdate(delayed_state ->
        {
            throw new Error("Simulated crash");
        });
        final AlarmState state = new AlarmState(SeverityLevel.MAJOR, "Test", null, Instant.now());
        delay.schedule_update(state, 1);

        // Wait for delay to expire
        Thread.sleep(2000);

        // Try to schedule another delayed update
        delay.schedule_update(state, 1);
        // It will also fail, but at least it's still possible to schedule another update. The Timer wasn't canceled.
        delay.cancel();
    }
}
