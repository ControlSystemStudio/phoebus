/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import org.epics.pva.PVASettings;
import org.epics.pva.data.PVAStructure;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;

/** Client demo for IPv6ServerDemo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class IPv6ClientDemo
{
    static
    {
        try
        {
            // Log everything
            LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));
            PVASettings.logger.setLevel(Level.ALL);

            PVASettings.EPICS_PVA_AUTO_ADDR_LIST = false;

            // Connect to IPv6ServerDemo via unicast
            PVASettings.EPICS_PVA_ADDR_LIST = "[::1]:5076";

            // IPv4 unicast
            // PVASettings.EPICS_PVA_ADDR_LIST = "127.0.0.1:5076";

            // IPv6 multicast on loopback
            // PVASettings.EPICS_PVA_ADDR_LIST = "[ff02::42:1],1@::1";

            // IPv4 multicast on loopback
            // PVASettings.EPICS_PVA_ADDR_LIST = "224.0.1.1,1@127.0.0.1";
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void testSimplestGet() throws Exception
    {
        try
        (   // Create a client and channel (will be auto-closed)
            final PVAClient pva = new PVAClient();
            final PVAChannel ch = pva.getChannel("demo");
        )
        {
            // Connect
            ch.connect().get(5, TimeUnit.SECONDS);

            // Get data
            final Future<PVAStructure> data = ch.read("");
            System.out.println(ch.getName() + " = " + data.get());
        }
    }

    @Test
    public void testMonitor() throws Exception
    {
        try
        (   // Create a client and channel (will be auto-closed)
            final PVAClient pva = new PVAClient();
            final PVAChannel ch = pva.getChannel("demo");
        )
        {
            // Connect
            ch.connect().get(5, TimeUnit.SECONDS);

            // Get data
            final AutoCloseable sub = ch.subscribe("", (channel, changes, overruns, data) ->
            {
                System.out.println(channel.getName() + " = " + data);
            });

            TimeUnit.SECONDS.sleep(5);
            sub.close();
            TimeUnit.SECONDS.sleep(5);
        }
    }
}
