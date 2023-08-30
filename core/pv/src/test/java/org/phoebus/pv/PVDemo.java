/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import org.epics.vtype.VType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * PV demos
 *
 * <p>Needs to be adjusted to site-specific PV names.
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVDemo {
    /**
     * Async read is an 'active', round-trip read.
     * For channel access, it can trigger processing on the IOC
     * and await the result.
     */
    @Test
    public void demoAsync() throws Exception {
        final PV pv = PVPool.getPV("SomePV");

        // The PV needs to be connected
        try {
            CountDownLatch connect = new CountDownLatch(1);
            pv.onValueEvent().subscribe(value ->
            {
                if (!PV.isDisconnected(value))
                    connect.countDown();
            });
            connect.await();

            // .. before an async read can be issued:
            System.out.println("Async read: " + pv.asyncRead().get(10, TimeUnit.SECONDS));
        } finally {
            PVPool.releasePV(pv);
        }
    }

    /**
     * Reading a value from multiple PVs
     */
    @Test
    public void demoReadMultiple() throws Exception {
        // Create PVs
        final List<PV> pvs = List.of(PVPool.getPV("SomePV1"),
                PVPool.getPV("SomePV2"),
                PVPool.getPV("SomePV3"),
                // ...
                PVPool.getPV("SomePV99"));

        try {
            // Subscribe to all of them
            final List<CompletableFuture<VType>> latest = new ArrayList<>();
            for (PV pv : pvs) {
                final CompletableFuture<VType> done = new CompletableFuture<>();
                latest.add(done);
                pv.onValueEvent().subscribe(value -> done.complete(value));
            }

            // Await update on all of them (or fail)
            // Somewhat unfortunate that 'allOf' uses array, not list...
            CompletableFuture.allOf(latest.toArray(new CompletableFuture[latest.size()]))
                    .get(10, TimeUnit.SECONDS);

            // Print all the values
            for (int i = 0; i < pvs.size(); ++i)
                System.out.println(pvs.get(i).getName() + " = " + latest.get(i).get());
        } finally {
            for (PV pv : pvs)
                PVPool.releasePV(pv);
        }
    }
}
