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
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVATimeStamp;

/** PVA Server that periodically drops a PV
 *
 *  <p>For testing if monitoring client re-connect
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class IntermittentPVDemo
{
    public static void main(String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));

        try (final PVAServer server = new PVAServer())
        {
            while (true)
            {
                // Start, run, stop server
                final PVATimeStamp time = new PVATimeStamp();
                final PVAStructure data = new PVAStructure("demo", "demo_t",
                                                           new PVADouble("value", 3.13),
                                                           new PVAString("tag",   "Hello!"),
                                                           time);

                try (final ServerPV pv = server.createPV("demo", data))
                {
                    System.out.println("Created " + pv.getName());
                    for (int i=0; i<10; ++i)
                    {
                        TimeUnit.MILLISECONDS.sleep(1000);

                        final PVADouble value = data.get("value");
                        value.set(value.get() + 1);
                        time.set(Instant.now());

                        pv.update(data);
                    }
                    System.out.println("Destroyed " + pv.getName());
                }

                // Wait a little, server down, then repeat
                TimeUnit.SECONDS.sleep(3);
            }
        }
    }
}
