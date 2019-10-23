/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.combined;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
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
import org.epics.pva.server.WriteEventHandler;

/** Start PVA Servers, write to their PVs
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WriteDemo
{
    // PVA Server for given PV name
    // Quits when writing a negative number to the PV
    static void serve(final String name)
    {
        try
        {
            final CountDownLatch done = new CountDownLatch(1);

            final PVAServer server = new PVAServer();

            // Construct a custom data type with 'value' and time stamp
            final PVAStructure data = new PVAStructure("demo", "demo_t",
                                                       new PVADouble("value", 3.13),
                                                       new PVATimeStamp());
            final WriteEventHandler write_handler = (pv, changes, written) ->
            {
                // Set timestamp, then take data as received
                PVATimeStamp.set(written, Instant.now());
                pv.update(written);

                System.out.println("Received " + name + " = " + written);
                if (((PVADouble)written.get(1)).get() < 0)
                    done.countDown();
            };
            server.createPV(name, data, write_handler);

            System.out.println(">>>>>>>>>> Serving " + name);
            done.await();

            server.close();
            System.out.println("<<<<<<<<<< Server for  " + name + " closed.");
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
        root.setLevel(Level.WARNING);
        for (Handler handler : root.getHandlers())
            handler.setLevel(root.getLevel());

        // Start PVA servers
        final ForkJoinTask<?> server1 = ForkJoinPool.commonPool().submit(() -> serve("demo1"));
        final ForkJoinTask<?> server2 = ForkJoinPool.commonPool().submit(() -> serve("demo2"));
        final ForkJoinTask<?> server3 = ForkJoinPool.commonPool().submit(() -> serve("demo3"));

        // PVA Client
        System.out.println("Writing value 5 .. -1 to PVs...");
        final PVAClient pva = new PVAClient();
        final PVAChannel ch1 = pva.getChannel("demo1");
        final PVAChannel ch2 = pva.getChannel("demo2");
        final PVAChannel ch3 = pva.getChannel("demo3");
        CompletableFuture.allOf(ch1.connect(), ch2.connect(), ch3.connect()).get();

        for (double v=5.0; v>=-1.0; --v)
        {
            TimeUnit.MILLISECONDS.sleep(100);
            ch1.write("", v);
            TimeUnit.MILLISECONDS.sleep(100);
            ch2.write("", v);
            TimeUnit.MILLISECONDS.sleep(100);
            ch3.write("", v);
        }
        System.out.println("Closing PVs");
        ch3.close();
        ch2.close();
        ch1.close();

        System.out.println("Waiting for servers to exit");
        server3.get(2, TimeUnit.SECONDS);
        server2.get(2, TimeUnit.SECONDS);
        server1.get(2, TimeUnit.SECONDS);

        System.out.println("Done.");
    }
}
