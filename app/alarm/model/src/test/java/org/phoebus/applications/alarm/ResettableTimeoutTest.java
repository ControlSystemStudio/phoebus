/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.phoebus.applications.alarm.ResettableTimeout;

/** JUnit test of the {@link ResettableTimeout}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ResettableTimeoutTest
{
    @Test
    public void testTimer() throws Exception
    {
        System.out.println("Check for no timeout ...");
        ResettableTimeout timer = new ResettableTimeout(5);
        // Has not timed out after 2 seconds
        assertThat(timer.awaitTimeout(2), equalTo(false));
        timer.shutdown();

        System.out.println("Check for timeout ...");
        timer = new ResettableTimeout(2);
        // Has certainly timed out after 4 seconds.
        // await..() should actually already return after 2 secs
        assertThat(timer.awaitTimeout(4), equalTo(true));
        timer.shutdown();
    }

    @Test
    public void testReset() throws Exception
    {
        System.out.println("Timeout in 4 secs?");
        final ResettableTimeout timer = new ResettableTimeout(4);
        final ScheduledExecutorService resetter = Executors.newSingleThreadScheduledExecutor();
        resetter.scheduleAtFixedRate(() ->
        {
            System.out.println("Reset..");
            timer.reset();
        }, 1, 1, TimeUnit.SECONDS);

        // Has not timed out after 8 seconds
        assertThat(timer.awaitTimeout(8), equalTo(false));
        resetter.shutdown();

        // Has timed out after 6 seconds
        System.out.println("Stopped the resets. Should now time out in 4 secs");
        assertThat(timer.awaitTimeout(6), equalTo(true));
        timer.shutdown();
    }
}
