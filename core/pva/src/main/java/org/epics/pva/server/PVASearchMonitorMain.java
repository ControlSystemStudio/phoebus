/*******************************************************************************
 * Copyright (c) 2022-2025 Oak Ridge National Laboratory.
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
import java.util.concurrent.atomic.AtomicBoolean;
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

        /** Client that searched for the PV */
        final InetSocketAddress client;

        /** Time of last search */
        volatile Instant last = null;

        SearchInfo(final String name, final InetSocketAddress client)
        {
            this.name = name;
            this.client = client;
        }

        /** @return Key of PV name and client address */
        public String getKey()
        {
            return name + client.toString();
        }

        /** Sort by search count */
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

    /** Map of PV name and client address to search info */
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
        final AtomicBoolean once = new AtomicBoolean();

        for (int i=0; i<args.length; ++i)
        {
            final String arg = args[i];
            if (arg.startsWith("-h"))
            {
                help();
                return;
            }
            if (arg.startsWith("-1"))
                once.set(true);
            else if (arg.startsWith("-p") && (i+1) < args.length)
            {
                update_period = Long.parseLong(args[i+1]);
                ++i;
            }
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
            // In "continuing" mode, quit when receiving search for name "QUIT"
            if (!once.get()  &&  name.equals("QUIT"))
                done.countDown();

            final SearchInfo candidate = new SearchInfo(name, addr);
            final SearchInfo search = searches.computeIfAbsent(candidate.getKey(), k -> candidate);
            search.count.incrementAndGet();
            search.last = Instant.now();

            // Done, don't proceed with default search handler
            return true;
        };

        try
        (   final PVAServer server = new PVAServer(search_handler)  )
        {
            System.out.println("Monitoring search requests for " + update_period + " seconds...");
            if (! once.get())
                System.out.println("Run 'pvget QUIT' to stop");
            while (! done.await(update_period, TimeUnit.SECONDS))
            {
                int max_name = 10, max_client = 10;
                for (SearchInfo info : searches.values())
                {
                    max_name   = Math.max(max_name,   info.name.length());
                    max_client = Math.max(max_client, info.client.toString().length());
                }

                System.out.format("\nCount %-" + max_name + "s %-" + max_client + "s    Age\n", "Name", "Last Client");
                final String format = "%5d %-" + max_name + "s %-" + max_client + "s %6d sec\n";
                final Instant now = Instant.now();
                searches.values()
                        .stream()
                        .sorted()
                        .forEach(info ->
                {
                    System.out.format(format,
                                      info.count.get(),
                                      info.name,
                                      info.client.toString(),
                                      now.getEpochSecond() - info.last.getEpochSecond());
                });
                if (once.get())
                    break;
            }
        }

        System.out.println("Done.");
    }
}
