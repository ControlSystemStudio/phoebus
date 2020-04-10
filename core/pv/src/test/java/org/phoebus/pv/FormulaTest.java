/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import io.reactivex.disposables.Disposable;

/** @author Kay Kasemir */
@SuppressWarnings("nls")
public class FormulaTest
{
    private void dumpPool()
    {
        System.out.println("PV Pool");
        PVPool.getPVReferences().forEach(System.out::println);
    }

    // A formula tends to contain references to other PVs.
    // Original PVPool code used ConcurrentHashMap in RefCountMap.
    // Adding a formula resulted in recursively adding the referenced PVs
    // within the same 'computeIfAbsent()' call,
    // which throws a 'Recursive update' exception and may also block,
    // causing this test to hang.
    // After update, this test completes.
    public void runFormula(final String formula) throws Exception
    {
        final CountDownLatch done = new CountDownLatch(2);

        final PV pv = PVPool.getPV(formula);
        final Disposable flow = pv.onValueEvent()
                                  .subscribe(value ->
        {
            System.out.println(formula + " = " + value);
            done.countDown();
        });
        done.await();
        flow.dispose();

        dumpPool();
        PVPool.releasePV(pv);
    }

    @Test(timeout = 10000)
    public void demoFormula() throws Exception
    {
        runFormula("=`sim://sine` * 2");
        runFormula("=`sim://ramp` + 100");
        dumpPool();
    }
}
