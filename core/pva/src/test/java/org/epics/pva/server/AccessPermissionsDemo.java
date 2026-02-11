/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.epics.pva.PVASettings;
import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVATimeStamp;

/** PVA server access permissions demo
 * *
 *  @author Kay Kasemir
 */
public class AccessPermissionsDemo
{
    private static final String PVLIST = "src/test/resources/demo.pvlist";
    private static final String ACF = "src/test/resources/demo2.acf";

    public static void main(String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));
        PVASettings.logger.setLevel(Level.FINE);
        Logger.getLogger("jdk.event.security").setLevel(PVASettings.logger.getLevel());

        try (final PVAServer server = new PVAServer())
        {
            server.configureAuthorization(new FileBasedServerAuthorization(PVLIST, ACF));

            // Create data structures to serve
            final PVATimeStamp time1 = new PVATimeStamp();
            final PVATimeStamp time2 = new PVATimeStamp();

            final PVADouble value1 = new PVADouble("value", 3.13);
            final PVAStructure data1 = new PVAStructure("demo", "demo_t",
                                                        value1,
                                                        time1);

            final PVADouble value2 = new PVADouble("value", 10.0);
            final PVAStructure data2 = new PVAStructure("demo", "demo_t",
                                                        value2,
                                                        time2);

            // Create PVs
            final ServerPV pv1 = server.createPV("ramp", data1);
            server.createPV("limit", data2, (tcp, pv, changes, written) ->
            {
                time2.set(Instant.now());
                PVADouble val = written.get("value");
                value2.set(val.get());
                pv.update(data2);
            });

            System.out.println("Check");
            System.out.println("    pvmonitor ramp");
            System.out.println("    pvput limit 5");
            System.out.println();
            System.out.println("'limit' is writable, for details see");
            System.out.println(PVLIST);
            System.out.println("and");
            System.out.println(ACF);


            // Update PVs
            while (true)
            {
                TimeUnit.SECONDS.sleep(1);

                // Update the data, tell server that it changed.
                // Server figures out what changed.
                double next = value1.get() + 1;
                if (next > value2.get())
                    next = 0.0;
                value1.set(next);
                time1.set(Instant.now());

                pv1.update(data1);
            }
        }
    }
}
