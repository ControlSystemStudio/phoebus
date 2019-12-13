/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;
import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVATimeStamp;

/** PVA Server Demo
 *
 *  <p>PVs "demo" and "demo2" updates.
 *  PV "demo3" is writable.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ServerDemo
{
    public static void main(String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));

        try
        (
            // Create PVA Server (auto-closed)
            final PVAServer server = new PVAServer();
        )
        {
            // Create data structures to serve
            final PVATimeStamp time = new PVATimeStamp();
            final PVAStructure data = new PVAStructure("demo", "demo_t",
                                                       new PVADouble("value", 3.13),
                                                       new PVAString("tag",   "Hello!"),
                                                       new PVAStructure("alarm", "alarm_t",
                                                                        new PVAInt("severity", 0),
                                                                        new PVAInt("status", 0),
                                                                        new PVAString("message", "OK")
                                                                       ),
                                                       time);

            final PVAStructure writable_data = data.cloneData();

            // Create PVs
            // Read-only
            final ServerPV pv1 = server.createPV("demo", data);
            final ServerPV pv2 = server.createPV("demo2", data);

            // Writable
            final ServerPV write_pv = server.createPV("demo3", writable_data, (pv, changes, written) ->
            {
                // Write handler could check what was changed,
                // clamp data to certain range etc.
                // Here we accept all and update the PV with the new data.
                PVATimeStamp.set(written, Instant.now());
                pv.update(written);
            });

            // Update PVs
            for (int i=0; i<30000; ++i)
            {
                TimeUnit.SECONDS.sleep(1);

                // Update the data, tell server that it changed.
                // Server figures out what changed.
                final PVADouble value = data.get("value");
                value.set(value.get() + 1);
                time.set(Instant.now());

                pv1.update(data);
                pv2.update(data);
            }

            // Note that updated data type must match the originally served data.
            // Cannot change the structure layout for existing PV.
            try
            {
                pv1.update(new PVAStructure("xx", "xxx", new PVAInt("xx", 47)));
            }
            catch (Exception ex)
            {
                // Expected
                if (! ex.getMessage().toLowerCase().contains("incompatibl"))
                    throw ex;
            }
        }
    }
}
