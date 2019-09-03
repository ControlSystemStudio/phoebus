/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;
import org.junit.Test;

/** Demo using 'neutrons' PV from
 *  https://github.com/kasemir/EPICSV4Sandbox/tree/master/neutronsDemoServer
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NeutronsDemo
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
    public void testNeutrons() throws Exception
    {
        // Create a client
        final PVAClient pva = new PVAClient();

        // Connect
        final ClientChannelListener channel_listener = (channel, state) -> System.out.println(channel);
        final PVAChannel ch = pva.getChannel("neutrons", channel_listener);
        while (ch.getState() != ClientChannelState.CONNECTED)
            TimeUnit.MILLISECONDS.sleep(100);

        // Get value
        // OK:
        // Hexdump [Get request] size = 47
        // 01 00 00 00  01 00 00 00  08 FD 01 00  80 00 01 05  .... .... .... ....
        // 66 69 65 6C  64 FD 02 00  80 00 01 0D  70 72 6F 74  fiel d... .... prot
        // 6F 6E 5F 63  68 61 72 67  65 FD 03 00  80 00 00     on_c harg e... ...

        // Not OK:
        // Hexdump [Get request] size = 15
        // 01 00 00 00  01 00 00 00  08 FD 01 00  80 00 01

        System.out.println(ch.read("proton_charge").get(2, TimeUnit.SECONDS));

        // Monitor updates
        MonitorListener monitor_listener = (channel, changes, overruns, data) ->
        {
            System.out.println(data);
        };
        final AutoCloseable subscription = ch.subscribe("proton_charge, timeStamp.secondsPastEpoch, pixel", monitor_listener);
        TimeUnit.SECONDS.sleep(5);
        subscription.close();

        // Close channels
        ch.close();

        // Close the client
        pva.close();
    }
}
