/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
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
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVATimeStamp;

/** PVA Server Demo
 *
 *  <p>PV "demo" with array of variable size, including empty.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArrayDemo
{
    public static void main(String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));

        try (final PVAServer server = new PVAServer())
        {
            // Create data structures to serve
            final PVATimeStamp time = new PVATimeStamp();
            final PVADoubleArray value = new PVADoubleArray("value", new double[] { 3.13 });
            final PVAStructure data = new PVAStructure("demo", "demo_t",
                                                       value,
                                                       time);

            // Create PVs
            final ServerPV pv1 = server.createPV("demo", data);

            // Update PVs
            for (int i=0; i<30000; ++i)
            {
                TimeUnit.SECONDS.sleep(1);

                // Update the data, tell server that it changed.
                // Server figures out what changed.
                final int len = i % 5;
                final double[] update = new double[len];
                for (int e=0; e<len; ++e)
                    update[e] = 3.13 + i + e/2.0;
                value.set(update);
                time.set(Instant.now());

                pv1.update(data);
            }
        }
    }
}
