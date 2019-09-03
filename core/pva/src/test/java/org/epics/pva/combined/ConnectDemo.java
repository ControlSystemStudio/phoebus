/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.combined;

import java.time.Instant;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.epics.pva.PVASettings;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVATimeStamp;
import org.epics.pva.server.PVAServer;
import org.epics.pva.server.ServerPV;

/** Start PVA Server, keep connecting, read, disconnect
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ConnectDemo
{
    static void serve(final String name, final TimeUnit update, final long delay)
    {
        try
        {
            final PVAServer server = new PVAServer();

            final PVATimeStamp time = new PVATimeStamp();
            final PVADouble value = new PVADouble("value", 3.13);
            final PVAStructure data = new PVAStructure("demo", "demo_t",
                                                       value,
                                                       time);
            final ServerPV pv = server.createPV(name, data);
            while (true)
            {
                update.sleep(delay);
                value.set(value.get() + 1);
                time.set(Instant.now());
                pv.update(data);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception
    {
        // Configure logging
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));
        final Logger root = Logger.getLogger("");
        root.setLevel(Level.CONFIG);
        for (Handler handler : root.getHandlers())
            handler.setLevel(root.getLevel());

        // Start PVA server
        ForkJoinPool.commonPool().submit(() -> serve("demo1", TimeUnit.SECONDS, 1));

        // PVA Client
        final PVAClient pva = new PVAClient();
        while (true)
        {
            System.err.println("\nCREATE CHANNEL ----------------------------");
            final PVAChannel ch = pva.getChannel("demo1");
            ch.connect().get();
            System.err.println("READ --------------------------------------");
            final PVAStructure data = ch.read("").get();
            System.err.println(ch.getName() + " = " + data.get("value"));
            System.err.println("CLOSE -------------------------------------\n");
            ch.close();
            // TimeUnit.SECONDS.sleep(1);
        }
    }
}
