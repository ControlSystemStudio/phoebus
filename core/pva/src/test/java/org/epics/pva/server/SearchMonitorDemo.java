/*******************************************************************************
 * Copyright (c) 2020-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;

/** PVA Server Demo that logs all received search requests
 *
 *  Can also demo a basic name server:
 *
 *  <pre>
 *  1) Start ioc as
 *     EPICS_PVA_SERVER_PORT=5078 EPICS_PVA_BROADCAST_PORT=5079  ./softIocPVA -m N='' -d src/test/resources/demo.db
 *
 *  2) Check that basic 'get' does not reach it
 *     pvxget ramp
 *
 *  3) Start this name server with command line args, either from IDE or like this:
 *     ant clean testlib
 *     java -cp target/core-pva-test.jar org.epics.pva.server.SearchMonitorDemo ramp 127.0.0.1:5078 saw 127.0.0.1:5078
 *
 *  4) Check that basic 'get' now reaches name server via UDP and then gets redirected to IOC
 *     pvxget ramp
 *
 *  5) Check 'get' also reaches name server via TCP and then gets redirected to IOC
 *     EPICS_PVA_BROADCAST_PORT=9876 EPICS_PVA_NAME_SERVERS="127.0.0.1:5076" ./pvxget ramp
 *  </pre>
 *
 *  Typical Server with PVs:
 *  <pre>
 *  UDP search -> UDP reply with this server's TCP address
 *  TCP search -> TCP reply with "0.0.0.0:0" to indicate "Use this TCP connection"
 *  </pre>
 *
 *  Name Server
 *  <pre>
 *  UDP search -> UDP reply with TCP address of the PV's host
 *  TCP search -> TCP reply with TCP address of the PV's host
 *  </pre>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SearchMonitorDemo
{
    public static void main(String[] args) throws Exception
    {
        System.setProperty("EPICS_PVAS_BROADCAST_PORT", "5076");
        System.setProperty("EPICS_PVA_SERVER_PORT", "5076");

        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));
        PVASettings.logger.setLevel(Level.ALL);

        // Configure the "name server" from list of name, addr, name, addr, ...
        final Map<String, InetSocketAddress> pvs = new HashMap<>();
        if (args.length % 2 != 0  ||
            (args.length > 0  &&  args[0].startsWith("-h")))
        {
            System.out.println("USAGE: SearchMonitorDemo name addr name addr name addr ...");
            System.out.println();
            System.out.println("  name: PV Name");
            System.out.println("  addr: Servers TCP address in form '127.0.0.1:port'");
            return;
        }
        for (int i=0; i<args.length; i += 2)
        {
            final String pv = args[i];
            final int sep = args[i+1].indexOf(':');
            if (sep < 0)
            {
                System.out.println("Cannot parse 'IP:port' from '" + args[i+1] + "'");
                return;
            }
            final InetSocketAddress addr = new InetSocketAddress(args[i+1].substring(0, sep),
                                                                 Integer.parseInt(args[i+1].substring(sep+1)));
            System.out.println(pv + " is served by " + addr);
            pvs.put(pv, addr);
        }


        // Start PVA server with custom search handler
        final CountDownLatch done = new CountDownLatch(1);
        final SearchHandler search_handler = (seq, cid, name, addr, reply_sender) ->
        {
            System.out.println(addr + " searches for " + name + " (seq " + seq + ")");
            // Quit when receiving search for name "QUIT"
            if (name.equals("QUIT"))
                done.countDown();

            // Check "name server"
            final InetSocketAddress server_addr = pvs.get(name);
            if (server_addr != null)
            {
                System.out.println(" --> Sending client to " + server_addr);
                reply_sender.accept(server_addr);
            }

            // Done, don't proceed with default search handler
            return true;
        };

        try
        (   final PVAServer server = new PVAServer(search_handler)  )
        {
            System.out.println("For UDP search, run 'pvget' or 'pvxget' with");
            System.out.println("EPICS_PVA_BROADCAST_PORT=" + PVASettings.EPICS_PVAS_BROADCAST_PORT);
            System.out.println("For TCP search, set EPICS_PVA_NAME_SERVERS = " + server.getTCPAddress());
            System.out.println("or other IP address of this host and same port.");
            System.out.println("Run 'pvget QUIT' to stop");
            done.await();
        }

        System.out.println("Done.");
    }
}
