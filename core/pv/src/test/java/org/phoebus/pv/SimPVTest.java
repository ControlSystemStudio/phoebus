/*******************************************************************************
 * Copyright (c) 2017-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.jupiter.api.Test;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** @author Kay Kasemir */
@SuppressWarnings("nls")
public class SimPVTest
{
    @Test
    public void demoSine() throws Exception
    {
        final CountDownLatch done = new CountDownLatch(3);

        System.out.println("Awaiting " + done.getCount() + " updates...");
        final PV pv = PVPool.getPV("sim://sine");
        final Disposable flow = pv.onValueEvent()
                                  .subscribe(value ->
        {
            System.out.println("Received update " + value);
            done.countDown();
        });
        done.await();
        flow.dispose();
        PVPool.releasePV(pv);
    }

    @Test
    public void demoSineWithTrimming1() throws Exception
    {
        final CountDownLatch done = new CountDownLatch(3);

        System.out.println("Awaiting " + done.getCount() + " updates...");
        final PV pv = PVPool.getPV("sim://sine\n");
        final Disposable flow = pv.onValueEvent()
                .subscribe(value ->
                {
                    System.out.println("Received update " + value);
                    done.countDown();
                });
        done.await();
        flow.dispose();
        PVPool.releasePV(pv);
    }

    @Test
    public void demoSineWithTrimming2() throws Exception
    {
        final CountDownLatch done = new CountDownLatch(3);

        System.out.println("Awaiting " + done.getCount() + " updates...");
        final PV pv = PVPool.getPV("sim://sine ");
        final Disposable flow = pv.onValueEvent()
                .subscribe(value ->
                {
                    System.out.println("Received update " + value);
                    done.countDown();
                });
        done.await();
        flow.dispose();
        PVPool.releasePV(pv);
    }

    @Test
    public void demoConst() throws Exception
    {
        final PV pv = PVPool.getPV("sim://const(3.14)");
        assertEquals(3.14, VTypeHelper.toDouble(pv.read()), 0.001);
        PVPool.releasePV(pv);
    }
}
