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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.epics.pva.PVASettings;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVAStructure;

/** Start multiple PVA Servers, keep connecting, read, disconnect
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MultipleServerDemo
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
        ForkJoinPool.commonPool().submit(() -> ConnectDemo.serve("demo1", TimeUnit.SECONDS, 1));
        ForkJoinPool.commonPool().submit(() -> ConnectDemo.serve("demo2", TimeUnit.SECONDS, 1));
        ForkJoinPool.commonPool().submit(() -> ConnectDemo.serve("demo3", TimeUnit.SECONDS, 1));

        // PVA Client
        final PVAClient pva = new PVAClient();
        while (true)
        {
            System.err.println("\nCREATE CHANNEL ----------------------------");
            final PVAChannel ch1 = pva.getChannel("demo1");
            final PVAChannel ch2 = pva.getChannel("demo2");
            final PVAChannel ch3 = pva.getChannel("demo3");
            CompletableFuture.allOf(ch1.connect(), ch2.connect(), ch3.connect()).get();
            System.err.println("READ --------------------------------------");
            final Future<PVAStructure> data1 = ch1.read(""),
                                       data2 = ch2.read(""),
                                       data3 = ch3.read("");
            System.err.println(ch1.getName() + " = " + data1.get().get("value"));
            System.err.println(ch2.getName() + " = " + data2.get().get("value"));
            System.err.println(ch3.getName() + " = " + data3.get().get("value"));
            System.err.println("CLOSE -------------------------------------\n");
            ch3.close();
            ch2.close();
            ch1.close();
            // TimeUnit.SECONDS.sleep(1);
        }
    }
}
