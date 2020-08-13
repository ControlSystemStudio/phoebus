/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.epics.vtype.VType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.phoebus.core.vtypes.VTypeHelper;

import io.reactivex.disposables.Disposable;

/** @author Kay Kasemir */
@SuppressWarnings("nls")
public class FormulaTest
{
    @BeforeClass
    public static void setup()
    {
        System.setProperty("java.util.logging.ConsoleHandler.formatter",
                           "java.util.logging.SimpleFormatter");
        // 1: date, 2: source, 3: logger, 4: level, 5: message, 6:thrown
        System.setProperty("java.util.logging.SimpleFormatter.format",
                           "%1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n");

        final Logger logger = Logger.getLogger("");
        logger.setLevel(Level.FINE);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(logger.getLevel());
    }

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

    // Formula with a 'local' PV that's updated by a 10Hz thread,
    // and some 'sim' PVs on a 1Hz thread.
    // With full logging on, observe how two quasi-concurrent
    // input updates result in only one formula re-evaluation.
    @Test
    public void concurrentInputs() throws Exception
    {
        final CountDownLatch done = new CountDownLatch(10);

        // Local PV, updated by thread different from 'sim' PV thread
        final PV loc = PVPool.getPV("loc://x(0)");
        final Thread update_loc = new Thread(() ->
        {
            try
            {
                while (! done.await(100, TimeUnit.MILLISECONDS))
                {
                    loc.write(System.currentTimeMillis());
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        });
        update_loc.start();

        // Formula triggered by loc and sim PVs
        final PV pv = PVPool.getPV("= `loc://x(0)` + `sim://noise` + 2 * `sim://ramp`");
        final Disposable flow = pv.onValueEvent()
                                  .subscribe(value ->
        {
            System.out.println(VTypeHelper.toDouble(value));
            done.countDown();
        });

        // Await some updates
        done.await();

        // Loc thread should finish
        update_loc.join();

        // Close everything
        flow.dispose();
        PVPool.releasePV(loc);
        PVPool.releasePV(pv);
        dumpPool();
    }

    @Test
    public void initialDisconnect() throws Exception
    {
        // Formula with missing PV needs to be 'disconnected'
        PV pv = PVPool.getPV("= `missing_PV` + 5");

        VType value = pv.read();
        System.out.println(pv.getName() + " = " + value);

        assertTrue(PV.isDisconnected(value));
        PVPool.releasePV(pv);


        // 'if' still evaluates OK, since the missing PV is not used
        pv = PVPool.getPV("= 1 ? 42 : `missing_PV`");

        value = pv.read();
        System.out.println(pv.getName() + " = " + value);

        assertEquals(42.0, VTypeHelper.toDouble(value), 0.1);
        assertFalse(PV.isDisconnected(value));
        PVPool.releasePV(pv);


        // This gets an error because the missing PV _is_ used
        pv = PVPool.getPV("= 0 ? 42 : `missing_PV`");

        value = pv.read();
        System.out.println(pv.getName() + " = " + value);

        assertTrue(PV.isDisconnected(value));
        PVPool.releasePV(pv);

        // Error because missing PV is needed for the condition
        pv = PVPool.getPV("=`missing_PV` ? 0 : 1");

        value = pv.read();
        System.out.println(pv.getName() + " = " + value);

        assertTrue(PV.isDisconnected(value));
        PVPool.releasePV(pv);
    }
}
