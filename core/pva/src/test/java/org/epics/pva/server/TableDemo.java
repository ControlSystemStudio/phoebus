/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAIntArray;
import org.epics.pva.data.PVAStringArray;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVATimeStamp;

/** PVA Server Demo
 *
 *  <p>PV "demo" with example table
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TableDemo
{
    public static void main(String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));

        try (final PVAServer server = new PVAServer())
        {
            // Create data structures to serve
            final PVATimeStamp time = new PVATimeStamp();
            final PVAStringArray labels = new PVAStringArray("labels", "sec", "nano", "M1X", "M1Y");
            final PVAIntArray sec = new PVAIntArray("secondsPastEpoch", true, 1, 2);
            final PVAIntArray nano = new PVAIntArray("nanoseconds", true, 3, 4);
            final PVADoubleArray m1x = new PVADoubleArray("m1x", 3.13, 3.15);
            final PVADoubleArray m1y = new PVADoubleArray("m1y", 31.15, 32.14);
            final PVAStructure data = new PVAStructure("demo", "epics:nt/NTTable:1.0",
                                                       labels,
                                                       new PVAStructure("value", "",
                                                                        sec,
                                                                        nano,
                                                                        m1x,
                                                                        m1y),
                                                       time);

            // Create PVs
            final ServerPV pv1 = server.createPV("demo", data);

            TimeUnit.DAYS.sleep(1);
        }
    }
}
