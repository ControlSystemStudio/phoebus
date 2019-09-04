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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.epics.pva.PVASettings;
import org.epics.pva.client.MonitorListener;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVATimeStamp;
import org.epics.pva.server.PVAServer;
import org.epics.pva.server.ServerPV;

/** Start PVA Server with one PV, monitor it, increment when monitor received.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LoopDemo
{
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

        // Start PVA server
        final PVAServer server = new PVAServer();

        final PVATimeStamp time = new PVATimeStamp();
        final PVADouble value = new PVADouble("value", 3.13);
        final PVAStructure data = new PVAStructure("demo", "demo_t",
                                                   value,
                                                   time);
        final ServerPV pv = server.createPV("demo", data);
        final Semaphore gotit = new Semaphore(0);
        ForkJoinPool.commonPool().submit(() ->
        {
            while (true)
            {
                // When monitor received a value...
                gotit.acquire();
                value.set(value.get() + 1);
                time.set(Instant.now());
                pv.update(data);
            }
        });


        // PVA Client
        final PVAClient pva = new PVAClient();
        final PVAChannel ch1 = pva.getChannel("demo");
        ch1.connect().get();

        final AtomicInteger updates = new AtomicInteger();
        final MonitorListener listener = (ch, changes, overruns, received) ->
        {
            updates.incrementAndGet();
            // System.out.println(ch.getName() + " = " + received.get("value") + " " + overruns);
            gotit.release();
        };
        ch1.subscribe("", listener );


        while (true)
        {
            TimeUnit.SECONDS.sleep(1);
            System.out.println(updates.getAndSet(0) + " loops per second");
        }
    }
}
