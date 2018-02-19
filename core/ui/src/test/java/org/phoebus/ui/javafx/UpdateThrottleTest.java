/*******************************************************************************
 * Copyright (c) 2014-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/** JUnit test of {@link RTPlotUpdateThrottle}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class UpdateThrottleTest
{
    final private AtomicInteger updates = new AtomicInteger();

    @Test(timeout=5000)
    public void testUpdateThrottle() throws Exception
    {
        final Runnable update = new Runnable()
        {
            @Override
            public void run()
            {
                System.out.println("<- Update!");
                updates.incrementAndGet();
                synchronized (this)
                {
                    notifyAll();
                }
            }
        };

        final UpdateThrottle throttle = new UpdateThrottle(1L, TimeUnit.SECONDS, update);
        // No initial updates
        assertThat(updates.get(), equalTo(0));

        // One trigger passes 'right away'
        System.out.println("-> Trigger..");
        throttle.trigger();
        // .. with some delay because it's on other thread
        Thread.sleep(100);
        assertThat(updates.get(), equalTo(1));

        // Follow-up triggers are delayed...
        System.out.println("-> Trigger..");
        throttle.trigger();
        System.out.println("-> Trigger..");
        throttle.trigger();
        System.out.println("-> Trigger..");
        throttle.trigger();
        assertThat(updates.get(), equalTo(1));

        // .. until the period passes
        TimeUnit.SECONDS.sleep(2);
        assertThat(updates.get(), equalTo(2));

        // Faster...
        throttle.setDormantTime(500, TimeUnit.MILLISECONDS);
        throttle.trigger();
        TimeUnit.MILLISECONDS.sleep(100);
        assertThat(updates.get(), equalTo(3));
        throttle.trigger();
        TimeUnit.MILLISECONDS.sleep(100);
        assertThat(updates.get(), equalTo(3));
        TimeUnit.MILLISECONDS.sleep(500);
        assertThat(updates.get(), equalTo(4));
    }
}
