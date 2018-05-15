/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Test;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueUtil;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/** Demos of the {@link FlowablePV}
 *
 *  <p>Timing related tests are hard to reproduce,
 *  to printouts of what one should see and only few assertions.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FlowablePVTest
{
    @Test
    public void demoPlain() throws Exception
    {
        final FlowablePV pv = new FlowablePV("sim://ramp(1, 1000, 0.1)");
        final Observable<VType> obs = Observable.fromPublisher(pv);

        System.out.println("Subscribe to 10Hz value until we get 10 updates");
        final CountDownLatch count = new CountDownLatch(10);
        Disposable dis = obs.subscribe(value ->
        {
            System.out.println(value);
            count.countDown();
        });
        count.await(10, TimeUnit.SECONDS);
        dis.dispose();

        pv.close();
    }

    @Test
    public void demoSingleValue() throws Exception
    {
        final FlowablePV pv = new FlowablePV("sim://ramp(1, 5, 0.1)");
        final Observable<VType> obs = Observable.fromPublisher(pv);

        System.out.println("Get just one value from 10Hz source");
        final VType the_one = obs.blockingFirst();
        System.out.println(the_one);

        pv.close();
    }

    @Test
    public void demoBuffer() throws Exception
    {
        final FlowablePV pv = new FlowablePV("sim://ramp(1, 5, 0.5)");
        final Observable<VType> obs = Observable.fromPublisher(pv);

        System.out.println("Buffer a 2Hz value every 2 seconds");
        System.out.println("Should see 3 updates, each with 4 values");
        final CountDownLatch count = new CountDownLatch(3);
        final Disposable dis = obs
            .map(vtype -> ValueUtil.timeOf(vtype).getTimestamp())
            .buffer(2, TimeUnit.SECONDS)
            .subscribe(value ->
            {
                System.out.println(value);
                count.countDown();
            });
        count.await();
        dis.dispose();

        pv.close();
    }

    @Test
    public void demoBoggedBuffer() throws Exception
    {
        final FlowablePV pv = new FlowablePV("sim://ramp(1, 1000, 0.2)");
        final Observable<VType> obs = Observable.fromPublisher(pv);
    
        System.out.println("Throttle a 5Hz value every 1 second, with a consumer that's stuck for 2 secs");
        System.out.println("Should see updates every 2 seconds with all the buffered values since last update");
    
        final CountDownLatch count = new CountDownLatch(5);
        final Disposable dis = obs
            .buffer(1, TimeUnit.SECONDS)
            .subscribe(value ->
            {
                count.countDown();
                System.out.println(value.stream().map(vtype -> ValueUtil.numericValueOf(vtype)).collect(Collectors.toList()));
                System.out.println("-- busy for 2 sec --");
                TimeUnit.SECONDS.sleep(2);
            });
        count.await();
        dis.dispose();
    
        pv.close();
    }

    @Test
    public void demoThrottle() throws Exception
    {
        final FlowablePV pv = new FlowablePV("sim://ramp(1, 1000, 0.1)");
        final Observable<VType> obs = Observable.fromPublisher(pv);

        System.out.println("Throttle a 10Hz value every 1 second");
        System.out.println("Should see 5 snapshots, one every second, with values that are 10 apart");
        final CountDownLatch count = new CountDownLatch(5);
        final Disposable dis = obs
            .throttleLast(1, TimeUnit.SECONDS)
            .subscribe(value ->
            {
                System.out.println(value);
                count.countDown();
            });
        count.await();
        dis.dispose();

        pv.close();
    }

    @Test
    public void demoBoggedThrottle() throws Exception
    {
        final FlowablePV pv = new FlowablePV("sim://ramp(1, 1000, 0.1)");
        final Observable<VType> obs = Observable.fromPublisher(pv);

        System.out.println("Throttle a 10Hz value every 1 second, with a consumer that's stuck for 2 secs");
        System.out.println("Should see updates with values that are 20 apart every 2 seconds");

        final CountDownLatch count = new CountDownLatch(5);
        final Disposable dis = obs
            .throttleLast(1, TimeUnit.SECONDS)
            .subscribe(value ->
            {
                count.countDown();
                System.out.println(value);
                System.out.println("-- busy for 2 sec --");
                TimeUnit.SECONDS.sleep(2);
            });
        count.await();
        dis.dispose();

        pv.close();
    }
}
