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
import org.epics.pva.data.PVALong;
import org.epics.pva.data.PVALongArray;
import org.epics.pva.data.PVAShortArray;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.PVAStructureArray;
import org.epics.pva.data.PVAUnion;
import org.epics.pva.data.PVAny;
import org.epics.pva.data.nt.PVATimeStamp;

/** 'pva://IMAGE' Server Demo
 *
 *  <p>Display with fixed value range of 0..10, not 'auto scaling'.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageDemo
{
    public static void main(String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));

        // A 10x10 shape
        final short[] pixel =
        {
            1, 1, 1, 1, 9, 9, 1, 1, 1, 1,
            1, 1, 1, 9, 1, 1, 9, 1, 1, 1,
            1, 1, 9, 1, 1, 1, 1, 1, 1, 1,
            1, 9, 9, 1, 1, 1, 1, 9, 9, 1,
            1, 9, 9, 1, 1, 1, 1, 1, 1, 1,
            1, 9, 9, 1, 1, 1, 1, 9, 9, 1,
            1, 1, 9, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 9, 1, 1, 9, 1, 1, 1,
            1, 1, 1, 1, 9, 9, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1
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

            final int width = 10, height = 10;
            final PVAStructure dim1 = new PVAStructure("", "dimension_t",
                                                       new PVAInt("size", width),
                                                       new PVAInt("offset", 0),
                                                       new PVAInt("fullSize", width),
                                                       new PVAInt("binning", 1),
                                                       new PVABool("reverse", false));
            final PVAStructure dim2 = new PVAStructure("", "dimension_t",
                                                       new PVAInt("size", height),
                                                       new PVAInt("offset", 0),
                                                       new PVAInt("fullSize", height),
                                                       new PVAInt("binning", 1),
                                                       new PVABool("reverse", false));
            final PVAShortArray val_pix = new PVAShortArray("shortValue", false, pixel);

            final PVAUnion value = new PVAUnion("value", "",
                                                2,
                                                new PVABoolArray("booleanValue"),
                                                new PVAByteArray("byteValue", false),
                                                val_pix,
                                                new PVAIntArray("intValue", false),
                                                new PVALongArray("longValue", false),
                                                new PVAByteArray("ubyteValue", true),
                                                new PVAShortArray("ushortValue", true),
                                                new PVAIntArray("uintValue", true),
                                                new PVALongArray("ulongValue", true),
                                                new PVAFloatArray("floatValue"),
                                                new PVADoubleArray("doubleValue"));
            final PVAInt id = new PVAInt("uniqueId", false, 0);

            final PVAStructure attr = new PVAStructure("", "epics:nt/NTAttribute:1.0",
                                                       new PVAString("name"),
                                                       new PVAny("value"),
                                                       new PVAString("descriptor"),
                                                       new PVAInt("sourceType"),
                                                       new PVAString("source"));

            final PVAStructure image = new PVAStructure("", "epics:nt/NTNDArray:1.0",
                                                        value,
                                                        new PVAStructure("codec", "codec_t",
                                                                         new PVAString("name", "test"),
                                                                         new PVAny("parameters", new PVAInt("", false, 42))),
                                                        new PVALong("compressedSize", false, width * height),
                                                        new PVALong("uncompressedSize", false, width * height),
                                                        new PVAStructureArray("dimension", dim1, dim1, dim2),
                                                        id,
                                                        datatime,
                                                        new PVAStructureArray("attribute", attr),
                                                        time);

            // Create read-only PV
            final ServerPV pv = server.createPV("IMAGE", image);

            // Update PVs
            for (int i=0; true; ++i)
            {
                TimeUnit.SECONDS.sleep(1);

                // Cycle value ('color') of pixels that are not '1' to 2..10
                final short[] new_pixel = new short[pixel.length];
                short v = (short) (2+(i % 10));
                // Rotate using int pixels, no interpolation between adjacent pixels,
                // so only multiples of 90deg look reasonable
                double angle = Math.toRadians(90*i);
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                for (int p=0; p<pixel.length; ++p)
                {
                    double sx = p % width - width/2;
                    double sy = p / width - height/2;
                    int dx = (int) Math.round(cos*sx - sin*sy) + width/2;
                    int dy = (int) Math.round(sin*sx + cos*sy) + height/2;
                    int dp = dx + width * dy;
                    if (dp < 0  ||  dp >= (width*height))
                        new_pixel[p] = 1;
                    else if (pixel[dp] == 1)
                        new_pixel[p] = 1;
                    else
                        new_pixel[p] = v;
                }

                val_pix.set(new_pixel);
                id.set(i);
                datatime.set(Instant.now());
                time.set(Instant.now());

                // Tell server that data changed.
                // Server figures out what exactly changed.
                pv.update(image);
            }
        }
    }
}
