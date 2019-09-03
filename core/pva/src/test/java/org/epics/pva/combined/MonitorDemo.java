/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.combined;

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

/** Start multiple PVA Servers, keep monitoring
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MonitorDemo
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

        // Start PVA servers
        ForkJoinPool.commonPool().submit(() -> ConnectDemo.serve("demo1", TimeUnit.MILLISECONDS, 10));
        ForkJoinPool.commonPool().submit(() -> ConnectDemo.serve("demo2", TimeUnit.MILLISECONDS, 10));
        ForkJoinPool.commonPool().submit(() -> ConnectDemo.serve("demo3", TimeUnit.MILLISECONDS, 10));

        // PVA Client
        final PVAClient pva = new PVAClient();
        final PVAChannel ch1 = pva.getChannel("demo1");
        final PVAChannel ch2 = pva.getChannel("demo2");
        final PVAChannel ch3 = pva.getChannel("demo3");
        CompletableFuture.allOf(ch1.connect(), ch2.connect(), ch3.connect()).get();

        final MonitorListener listener = (ch, changes, overruns, data) ->
        {
            System.out.println(ch.getName() + " = " + data.get("value") + " " + overruns);
        };
        ch1.subscribe("", listener );
        ch2.subscribe("", listener);
        ch3.subscribe("", listener);

        synchronized (MonitorDemo.class)
        {
            MonitorDemo.class.wait();
        }
    }
}
