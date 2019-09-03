/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;
import org.epics.pva.data.PVAStructure;
import org.junit.Test;

/** Demo using demo.db from test resources
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TCPHandlerShutdownDemo
{
    static
    {
        try
        {
            LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void testTCPHandling() throws Exception
    {
        final PVAClient pva = new PVAClient();

        final PVAChannel ch1 = pva.getChannel("ramp");
        assertTrue(ch1.connect().get(5, TimeUnit.SECONDS));
        Future<PVAStructure> data = ch1.read("");
        System.out.println(ch1.getName() + " = " + data.get());
        ch1.close();

        // TCP connection is kept open...
        TimeUnit.SECONDS.sleep(5);
        assertTrue(pva.haveTCPConnections());

        // .. and re-used for other channel
        final PVAChannel ch2 = pva.getChannel("saw");
        assertTrue(ch2.connect().get(5, TimeUnit.SECONDS));
        data = ch2.read("");
        System.out.println(ch2.getName() + " = " + data.get());
        ch2.close();

        // Eventually, the unused connection is closed.
        System.out.println("Waiting for TCP handlers to shut down...");
        while (pva.haveTCPConnections())
            TimeUnit.SECONDS.sleep(1);
        System.out.println("All TCP handlers closed.");

        pva.close();
    }
}
