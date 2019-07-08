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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.epics.pva.PVASettings;
import org.epics.pva.client.MonitorListener;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVATimeStamp;
import org.epics.pva.server.PVAServer;
import org.epics.pva.server.ServerPV;

/** Start multiple PVA Servers each with a large array, keep monitoring
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArrayMonitorDemo
{
    static void serve(final String name, final TimeUnit update, final long delay)
    {
        try
        {
            final PVAServer server = new PVAServer();

            final PVATimeStamp time = new PVATimeStamp();
            final PVADoubleArray value = new PVADoubleArray("value");
            final PVAStructure data = new PVAStructure("demo", "demo_t",
                                                       value,
                                                       time);
            final ServerPV pv = server.createPV(name, data);
            double number = 1.0;
            while (true)
            {
                update.sleep(delay);
                ++number;
                final double[] array = new double[1000000];
                for (int i=0; i<array.length; ++i)
                    array[i] = number + 0.1 * i;
                value.set(array);
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
        // Profiler shows blocking in ConsoleHandler,
        // so reduce log messages to only warnings for performance tests
        root.setLevel(Level.WARNING);
        for (Handler handler : root.getHandlers())
            handler.setLevel(root.getLevel());

        // Start PVA servers
        ForkJoinPool.commonPool().submit(() -> serve("demo1", TimeUnit.MILLISECONDS, 10));
        ForkJoinPool.commonPool().submit(() -> serve("demo2", TimeUnit.MILLISECONDS, 10));
        ForkJoinPool.commonPool().submit(() -> serve("demo3", TimeUnit.MILLISECONDS, 10));

        // PVA Client
        final PVAClient pva = new PVAClient();
        final PVAChannel ch1 = pva.getChannel("demo1");
        final PVAChannel ch2 = pva.getChannel("demo2");
        final PVAChannel ch3 = pva.getChannel("demo3");
        CompletableFuture.allOf(ch1.connect(), ch2.connect(), ch3.connect()).get();

        final MonitorListener listener = (ch, changes, overruns, data) ->
        {
            // System.out.println(ch.getName() + " = " + data.get("value") + " " + overruns);
            PVADoubleArray array = data.get("value");
            System.out.println(ch.getName() + " = " + array.get().length + " " + overruns);
        };
        ch1.subscribe("", listener );
        ch2.subscribe("", listener);
        ch3.subscribe("", listener);

        synchronized (ArrayMonitorDemo.class)
        {
            ArrayMonitorDemo.class.wait();
        }
    }
}
