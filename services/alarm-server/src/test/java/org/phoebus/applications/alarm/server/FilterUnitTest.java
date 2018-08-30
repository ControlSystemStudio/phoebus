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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

/** JUnit test of the {@link Filter}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FilterUnitTest
{
    private AtomicInteger updates = new AtomicInteger();

    // Most recent value from filter.
    // SYNC on this
    private double last_value = Double.NaN;

    @Before
    public void setup()
    {
        // Configure logging to show 'all'
        final Logger logger = Logger.getLogger("");
        logger.setLevel(Level.ALL);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(Level.ALL);

        // Disable some messages
        Logger.getLogger("com.cosylab.epics").setLevel(Level.SEVERE);
    }

    public void filterChanged(final double value)
    {
        System.err.println("Filter evaluates to " + value);
        updates.incrementAndGet();
        synchronized (this)
        {
            last_value  = value;
            notifyAll();
        }
    }

    @Test(timeout=8000)
    public void testFilter() throws Exception
    {
        // Create local PVs
        final PV x = PVPool.getPV("loc://x(1.0)");
        final PV y = PVPool.getPV("loc://y(2.0)");
        // Set initial value because another test may already have used those vars..
        x.write(1.0);
        y.write(2.0);

        final Filter filter = new Filter("'loc://x(1.0)' + 'loc://y(2.0)'", this::filterChanged);
        filter.start();

        // Await initial value
        // FINER: Filter 'loc://x(1.0)' + 'loc://y(2.0)': loc://y = 2.0
        // --> Filter evaluates to 3.0
        synchronized (this)
        {
            while (last_value != 3.0)
                wait();
        }
        System.err.println("Received " + updates.get() + " updates");

        // May get update for this.. (2 or 3), or not
        x.write(4.0);
        // Definite update for both values: Anything from 2 to 4
        y.write(6.0);

        // Before the Filter.TIMER was added, occasionally saw this:
        // FilterPVhandler accept FINER: Filter 'loc://x(1.0)' + 'loc://y(2.0)': loc://x = 4.0
        // FilterPVhandler accept FINER: Filter 'loc://x(1.0)' + 'loc://y(2.0)': loc://y = 6.0
        // Filter evaluate FINER: Filter evaluates to 6.0 (previous value 3.0) on Thread[RxComputationThreadPool-4,5,main]
        // Filter evaluate FINER: Filter evaluates to 10.0 (previous value 6.0) on Thread[RxComputationThreadPool-5,5,main]
        // --> FilterPVhandler was called for the 2 PV updates
        //     and Filter.evaluate() computed in expected order

        // Filter evaluates to 10.0
        // Filter evaluates to 6.0
        // --> FilterPVhandler is called on different threads,
        //     and thus the evaluate -> listener calls happen out of order..

        synchronized (this)
        {
            while (last_value != 10.0)
                wait();
        }
        System.err.println("Received " + updates.get() + " updates");

        filter.stop();
        PVPool.releasePV(y);
        PVPool.releasePV(x);
    }

    @Test(timeout=8000)
    public void testUpdates() throws Exception
    {
        // Create local PVs
        final PV x = PVPool.getPV("loc://x(1.0)");
        // Set initial value because another test may already have used those vars..
        x.write(1.0);

        final Filter filter = new Filter("'loc://x(1.0)' < 5 ? 1 : 2", this::filterChanged);
        filter.start();

        // Wait for initial value
        synchronized (this)
        {
            while (last_value != 1.0)
                wait();
        }
        final int received_updates = updates.get();
        System.err.println("Received " + received_updates + " updates");

        // Variable changes, but result of formula doesn't, so there shouldn't be an update
        x.write(2.0);
        TimeUnit.SECONDS.sleep(2);
        assertThat(updates.get(), equalTo(received_updates));

        // Once the value changes, there should be another update
        x.write(6.0);
        synchronized (this)
        {
            while (last_value != 2.0)
                wait();
        }
        System.err.println("Received " + updates.get() + " updates");
        assertThat(updates.get(), equalTo(received_updates + 1));

        filter.stop();

        PVPool.releasePV(x);
    }

    @Test(timeout=50000)
    public void testPVError() throws Exception
    {
        synchronized (this)
        {
            last_value = Double.NaN;
        }

        final Filter filter = new Filter("'ca://bogus_pv_name' * 2", this::filterChanged);
        filter.start();

        System.err.println("Waiting for timeout from bogus PV name...");

        // Default time out is 30 seconds
        // Should not get any updates while waiting for the connection...
        TimeUnit.SECONDS.sleep(30 / 2);
        assertThat(updates.get(), equalTo(0));

        synchronized (this)
        {   // Last value should remain unchanged
            assertThat(Double.isNaN(last_value), equalTo(true));
        }

        filter.stop();
    }

//    @Test
//    public void keepRunning() throws Exception
//    {
//        while (true)
//            testFilter();
//    }
}
