/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import java.util.concurrent.CountDownLatch;

/** Write elements of data served by core-pva/src/test/java/org/epics/pva/server/BoolDemo.java
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVACustomStructDemo
{
    public static void main(String[] args) throws Exception
    {
        final PV pv = PVPool.getPV("pva://struct/flag2");

        try
        {
            // Await connection
            CountDownLatch connect = new CountDownLatch(1);
            pv.onValueEvent().subscribe(value ->
            {
                System.out.println(pv.getName() + " = " + value);
                if (!PV.isDisconnected(value))
                    connect.countDown();
            });
            connect.await();

            pv.asyncWrite(true).get();
        }
        finally
        {
            PVPool.releasePV(pv);
        }
    }
}
