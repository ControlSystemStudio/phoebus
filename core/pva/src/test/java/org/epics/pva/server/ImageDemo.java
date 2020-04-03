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
import org.epics.pva.data.PVABool;
import org.epics.pva.data.PVABoolArray;
import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAFloatArray;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAIntArray;
import org.epics.pva.data.PVALongArray;
import org.epics.pva.data.PVAShortArray;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.PVAStructureArray;
import org.epics.pva.data.PVAUnion;
import org.epics.pva.data.nt.PVATimeStamp;

/** 'pva://IMAGE' Server Demo
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageDemo
{
    public static void main(String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));

        // A 10x10 'C' type shape
        final short[] pixel =
        {
            1, 1, 1, 1, 9, 9, 1, 1, 1, 1,
            1, 1, 1, 9, 1, 1, 9, 1, 1, 1,
            1, 1, 9, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 9, 1, 1, 1, 1, 1, 1, 1,
            1, 9, 9, 1, 1, 1, 1, 1, 1, 1,
            1, 9, 9, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 9, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 9, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 9, 1, 1, 9, 1, 1, 1,
            1, 1, 1, 1, 9, 9, 1, 1, 1, 1,
        };

        try
        (
            // Create PVA Server (auto-closed)
            final PVAServer server = new PVAServer();
        )
        {
            // Create data structure
            final PVATimeStamp datatime = new PVATimeStamp("dataTimeStamp");
            final PVATimeStamp time = new PVATimeStamp();

            final PVAStructure dim1 = new PVAStructure("", "dimension_t",
                                                       new PVAInt("size", 10),
                                                       new PVAInt("offset", 0),
                                                       new PVAInt("fullSize", 10),
                                                       new PVAInt("binning", 1),
                                                       new PVABool("reverse", false));
            final PVAStructure dim2 = new PVAStructure("", "dimension_t",
                                                       new PVAInt("size", 10),
                                                       new PVAInt("offset", 0),
                                                       new PVAInt("fullSize", 10),
                                                       new PVAInt("binning", 1),
                                                       new PVABool("reverse", false));

            final PVAUnion value = new PVAUnion("value", "",
                                                2,
                                                new PVABoolArray("booleanValue"),
                                                new PVAByteArray("byteValue", false),
                                                new PVAShortArray("shortValue", false, pixel),
                                                new PVAIntArray("intValue", false),
                                                new PVALongArray("longValue", false),
                                                new PVAByteArray("ubyteValue", true),
                                                new PVAShortArray("ushortValue", true),
                                                new PVAIntArray("uintValue", true),
                                                new PVALongArray("ulongValue", true),
                                                new PVAFloatArray("floatValue"),
                                                new PVADoubleArray("doubleValue"));

            final PVAStructure image = new PVAStructure("", "epics:nt/NTNDArray:1.0",
                                                        value,
                                                        new PVAStructureArray("dimension", dim1, dim1, dim2),
                                                        datatime,
                                                        time);

            // Create read-only PV
            final ServerPV pv = server.createPV("IMAGE", image);

            // Update PVs
            for (int i=0; i<30000; ++i)
            {
                TimeUnit.SECONDS.sleep(1);

                // Cycle value ('color') of pixels that are not '1' to 2..10
                short v = (short) (2+(i % 10));
                for (int p=0; p<pixel.length; ++p)
                    if (pixel[p] != 1)
                        pixel[p] = v;

                datatime.set(Instant.now());
                time.set(Instant.now());

                // Tell server that data changed.
                // Server figures out what exactly changed.
                pv.update(image);
            }
        }
    }
}
