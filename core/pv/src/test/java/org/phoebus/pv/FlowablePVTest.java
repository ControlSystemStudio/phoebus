/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueUtil;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/** @author Kay Kasemir */
@SuppressWarnings("nls")
public class FlowablePVTest
{
    @Test
    public void demoPlain() throws Exception
    {
        final FlowablePV pv = new FlowablePV("sim://ramp(1, 5, 0.1)");
        final Observable<VType> obs = Observable.fromPublisher(pv);

        System.out.println("Subscribe to 10Hz value for 1 second");
        Disposable dis = obs.subscribe(System.out::println);
        TimeUnit.SECONDS.sleep(1);
        dis.dispose();

        pv.close();
    }

    @Test
    public void demoSingleValue() throws Exception
    {
        final FlowablePV pv = new FlowablePV("sim://ramp(1, 5, 0.1)");
        final Observable<VType> obs = Observable.fromPublisher(pv);

        System.out.println("Get just one value from 10Hz source");
        VType the_one = obs.blockingFirst();
        System.out.println(the_one);

        pv.close();
    }

    @Test
    public void demoBuffer() throws Exception
    {
        final FlowablePV pv = new FlowablePV("sim://ramp(1, 5, 0.5)");
        final Observable<VType> obs = Observable.fromPublisher(pv);

        System.out.println("Buffer a 2Hz value every 2 seconds");
        Disposable dis = obs
            .map(vtype -> ValueUtil.timeOf(vtype).getTimestamp())
            .buffer(2, TimeUnit.SECONDS)
            .subscribe(System.out::println);
        TimeUnit.SECONDS.sleep(5);
        dis.dispose();

        pv.close();
    }

    @Test
    public void demoThrottle() throws Exception
    {
        final FlowablePV pv = new FlowablePV("sim://ramp(1, 5, 0.5)");
        final Observable<VType> obs = Observable.fromPublisher(pv);

        System.out.println("Throttle a 2Hz value every 2 seconds");
        Disposable dis = obs
            .map(vtype -> ValueUtil.timeOf(vtype).getTimestamp())
            .throttleLast(2, TimeUnit.SECONDS)
            .subscribe(System.out::println);
        TimeUnit.SECONDS.sleep(5);
        dis.dispose();

        pv.close();
    }
}
