/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.epics.pva.PVASettings;
import org.junit.Test;

/** Beacon demo using demo.db from test resources
 *
 *  <p>To see beacons, might need to disable firewall
 *  since broadcasts are otherwise not received.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BeaconDemo
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
        final Logger root = Logger.getLogger("");
        root.setLevel(Level.FINE);
        for (Handler handler : root.getHandlers())
            handler.setLevel(root.getLevel());
    }

    @Test
    public void demoBeacons() throws Exception
    {
        // Create a client
        final PVAClient pva = new PVAClient();

        // Counters for connection and close states
        final CountDownLatch connected = new CountDownLatch(2);

        // Connect to one or more channels
        final ClientChannelListener listener = (channel, state) ->
        {
            System.out.println(channel);
            if (state == ClientChannelState.CONNECTED)
                connected.countDown();
        };
        final PVAChannel ch1 = pva.getChannel("ramp", listener);
        final PVAChannel ch2 = pva.getChannel("bogus", listener);

        System.out.println("Waiting forever to connect to 'bogus' channel");
        connected.await();

        // Close channels
        ch2.close();
        ch1.close();

        // Close the client
        pva.close();
    }
}
