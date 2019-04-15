/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.epics.vtype.Time;
import org.epics.vtype.VNumber;
import org.epics.vtype.VType;
import org.junit.Test;

import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.Disposable;

/** Demos of the {@link PV}'s "Reactive" API
 *
 *  <p>Timing related tests are hard to reproduce,
 *  to printouts of what one should see and only few assertions.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ReactivePVTest
{
    @Test
    public void demoPlain() throws Exception
    {
        final PV pv = PVPool.getPV("sim://ramp(1, 1000, 0.1)");
         System.out.println("Subscribe to 10Hz value until we get 10 updates");
        final CountDownLatch count = new CountDownLatch(10);
        final Disposable dis = pv
            .onValueEvent(BackpressureStrategy.BUFFER)
            .subscribe(value ->
        {
            System.out.println(value);
            count.countDown();
        });
        count.await(10, TimeUnit.SECONDS);
        dis.dispose();

        PVPool.releasePV(pv);
    }

    @Test
    public void demoSingleValue() throws Exception
    {
        final PV pv = PVPool.getPV("sim://ramp(1, 5, 0.1)");

        System.out.println("Get just one value from 10Hz source");
        final VType the_one = pv
            .onValueEvent(BackpressureStrategy.LATEST)
            .firstElement()
            .blockingGet();
        System.out.println(the_one);

        PVPool.releasePV(pv);
    }

    @Test
    public void demoBuffer() throws Exception
    {
        final PV pv = PVPool.getPV("sim://ramp(1, 5, 0.5)");

        System.out.println("Buffer a 2Hz value every 2 seconds");
        System.out.println("Should see 3 updates, each with 4 values");
        final CountDownLatch count = new CountDownLatch(3);
        final Disposable dis = pv
            .onValueEvent(BackpressureStrategy.BUFFER)
            .map(vtype -> Time.timeOf(vtype).getTimestamp())
            .buffer(2, TimeUnit.SECONDS)
            .subscribe(value ->
            {
                System.out.println(value);
                count.countDown();
            });
        count.await();
        dis.dispose();

        PVPool.releasePV(pv);
    }

    private static double numericValueOf(final VType vtype)
    {
        if (vtype instanceof VNumber)
            return ((VNumber)vtype).getValue().doubleValue();
        return Double.NaN;
    }

    @Test
    public void demoBoggedBuffer() throws Exception
    {
        final PV pv = PVPool.getPV("sim://ramp(1, 1000, 0.2)");

        System.out.println("Throttle a 5Hz value every 1 second, with a consumer that's stuck for 2 secs");
        System.out.println("Should see updates every 2 seconds with all the buffered values since last update");

        final AtomicReference<Double> last = new AtomicReference<>();
        final CountDownLatch count = new CountDownLatch(5);
        // In spite of 'LATEST' on the source,
        // the following 'buffer()' will consume from the source so nothing is dropped
        final Disposable dis = pv
            .onValueEvent(BackpressureStrategy.LATEST)
            .buffer(1, TimeUnit.SECONDS)
            .subscribe(value ->
            {
                count.countDown();
                final List<Double> values = value.stream().map(vtype -> numericValueOf(vtype)).collect(Collectors.toList());
                System.out.println(values);
                final Double previous = last.getAndSet(values.get(values.size() - 1));
                if (previous != null)
                    assertThat(previous + 1, equalTo(values.get(0)));
                System.out.println("-- busy for 2 sec --");
                try
                {
                    TimeUnit.SECONDS.sleep(2);
                }
                catch (InterruptedException ex)
                {
                    System.out.println("Interrupted");
                }
            });
        count.await();
        dis.dispose();

        PVPool.releasePV(pv);
    }

    @Test
    public void demoThrottle() throws Exception
    {
        final PV pv = PVPool.getPV("sim://ramp(1, 1000, 0.1)");

        System.out.println("Throttle a 10Hz value every 1 second");
        System.out.println("Should see 5 snapshots, one every second, with values that are 10 apart");
        final CountDownLatch count = new CountDownLatch(5);
        // In spite of BUFFER on the source, the later throttle..() drops samples as desired
        final Disposable dis = pv
            .onValueEvent(BackpressureStrategy.BUFFER)
            .throttleLast(1, TimeUnit.SECONDS)
            .subscribe(value ->
            {
                System.out.println(value);
                count.countDown();
            });
        count.await();
        dis.dispose();

        PVPool.releasePV(pv);
    }

    @Test
    public void demoThrottleLastVsLatest() throws Exception
    {
        final PV pv = PVPool.getPV("sim://ramp(1, 1000, 1)");

        System.out.println("Throttle a 1Hz value as 'Last' vs. 'Latest' every 3 seconds");
        System.out.println("'Latest' receives the first value, while 'Last' has initial latency.");
        System.out.println("From then on they throttle in similar way");

        final Disposable last = pv
            .onValueEvent(BackpressureStrategy.BUFFER)
            .throttleLast(3, TimeUnit.SECONDS)
            .subscribe(value ->
            {
                System.out.println("Last  : " + value + " @ " + Instant.now());
            });
        final Disposable latest = pv
            .onValueEvent(BackpressureStrategy.BUFFER)
            .throttleLatest(3, TimeUnit.SECONDS)
            .subscribe(value ->
            {
                System.out.println("Latest: " + value + " @ " + Instant.now());
            });

        Thread.sleep(11000);
        latest.dispose();
        last.dispose();

        PVPool.releasePV(pv);
    }






    @Test
    public void demoBoggedThrottle() throws Exception
    {
        final PV pv = PVPool.getPV("sim://ramp(1, 1000, 0.1)");

        System.out.println("Throttle a 10Hz value every 1 second, with a consumer that's stuck for 2 secs");
        System.out.println("Should see updates with values that are 20 apart every 2 seconds");

        final CountDownLatch count = new CountDownLatch(5);
        final Disposable dis = pv
            .onValueEvent(BackpressureStrategy.LATEST)
            .throttleLast(1, TimeUnit.SECONDS)
            .subscribe(value ->
            {
                count.countDown();
                System.out.println(value);
                System.out.println("-- busy for 2 sec --");
                try
                {
                    TimeUnit.SECONDS.sleep(2);
                }
                catch (InterruptedException ex)
                {
                    System.out.println("Interrupted");
                }
            });
        count.await();
        dis.dispose();

        PVPool.releasePV(pv);
    }

    @Test
    public void testReadOnlyAccess() throws Exception
    {
        final PV pv = PVPool.getPV("sim://ramp(1, 1000, 0.1)");
        final Boolean can_write = pv
            .onAccessRightsEvent()
            .firstElement()
            .blockingGet();
        assertThat(can_write, equalTo(false));

        PVPool.releasePV(pv);
    }

    @Test
    public void testWriteAccess() throws Exception
    {
        final PV pv = PVPool.getPV("loc://x(50)");
        final Boolean can_write = pv
            .onAccessRightsEvent()
            .firstElement()
            .blockingGet();
        assertThat(can_write, equalTo(true));

        PVPool.releasePV(pv);
    }

}
