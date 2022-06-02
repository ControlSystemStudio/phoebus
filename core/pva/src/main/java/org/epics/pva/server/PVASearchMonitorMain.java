/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;

/** PVA Search monitor
 *
 *  May be called as
 *    java -cp core-pva.jar org.epics.pva.server.PVASearchMonitorMain
 *  or from Phoebus product via
 *    phoebus.sh -main org.epics.pva.server.PVASearchMonitorMain
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVASearchMonitorMain
{
    /** Recent search */
    private static class SearchInfo implements Comparable<SearchInfo>
    {
        /** PV Name */
        final String name;

        /** How often some client has searched for it */
        final AtomicLong count = new AtomicLong();

        /** Client that searched most recently */
        volatile InetSocketAddress client;

        /** Time of last search */
        volatile Instant last = null;

        SearchInfo(final String name)
        {
            this.name = name;
        }

        @Override
        public int compareTo(final SearchInfo other)
        {
            final long diff = other.count.get() - count.get();
            if (diff < 0)
                return -1;
            if (diff > 0)
                return 1;
            return 0;
        }
    }

    /** Map of PV name to search info */
    private static final ConcurrentHashMap<String, SearchInfo> searches = new ConcurrentHashMap<>();

    private static void help()
    {
        System.out.println("USAGE: pvasearchmonitor [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h             Help");
        System.out.println("  -p <seconds>   Update period (default 10 seconds)");
        System.out.println("  -1             Update once, then quit");
        System.out.println("  -v <level>     Verbosity, level 0-5");
        System.out.println();
        System.out.println("Waits for the specified period,");
        System.out.println("then prints information about received search requests.");
    }

    private static void setLogLevel(final Level level)
    {
        PVASettings.logger.setLevel(level);
    }

    /** Command line entry point
     *  @param args Arguments
     *  @throws Exception on error
     */
    public static void main(String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));
        setLogLevel(Level.WARNING);
        long update_period = 10;
        boolean once = false;

        for (int i=0; i<args.length; ++i)
        {
            final String arg = args[i];
            if (arg.startsWith("-h"))
            {
                help();
                return;
            }
            if (arg.startsWith("-1"))
                once = true;
            else if (arg.startsWith("-p") && (i+1) < args.length)
                update_period = Long.parseLong(args[i+1]);
            else if (arg.startsWith("-v") && (i+1) < args.length)
            {
                switch (Integer.parseInt(args[i+1]))
                {
                case 0:
                    setLogLevel(Level.WARNING);
                    break;
                case 1:
                    setLogLevel(Level.CONFIG);
                    break;
                case 2:
                    setLogLevel(Level.INFO);
                    break;
                case 3:
                    setLogLevel(Level.FINE);
                    break;
                case 4:
                    setLogLevel(Level.FINER);
                    break;
                case 5:
                default:
                    setLogLevel(Level.ALL);
                }
                ++i;
            }
        }

        final CountDownLatch done = new CountDownLatch(1);

        final SearchHandler search_handler = (seq, cid, name, addr, reply_sender) ->
        {
            // Quit when receiving search for name "QUIT"
            if (name.equals("QUIT"))
                done.countDown();

            final SearchInfo search = searches.computeIfAbsent(name, n -> new SearchInfo(name));
            search.count.incrementAndGet();
            search.last = Instant.now();
            search.client = addr;

            // Done, don't proceed with default search handler
            return true;
        };

        try
        (   final PVAServer server = new PVAServer(search_handler)  )
        {
            System.out.println("Monitoring search requests for " + update_period + " seconds...");
            if (! once)
                System.out.println("Run 'pvget QUIT' to stop");
            while (! done.await(update_period, TimeUnit.SECONDS))
            {
                System.out.println("\nCount Name                 Last Client                                Age");
                final Instant now = Instant.now();
                searches.values()
                        .stream()
                        .sorted()
                        .forEach(info ->
                {
                    System.out.format("%5d %-20s %-35s %6d sec\n",
                                      info.count.get(),
                                      info.name,
                                      info.client.toString(),
                                      now.getEpochSecond() - info.last.getEpochSecond());
                });
                if (once)
                    break;
            }
        }

        System.out.println("Done.");
    }
}
