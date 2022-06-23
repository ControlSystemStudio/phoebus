/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import static org.phoebus.pv.PV.logger;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVAData;

/** PVA NDArray codec for JPEG-compressed data
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JPEGCodec extends Codec
{
    @Override
    public PVAData decompress(PVAByteArray value, int orig_data_type, int value_count) throws Exception
    {
        // Area detector codec plugin only supports JPEG for 8 bit data types
        if (orig_data_type == 1  ||  orig_data_type == 5)
            return super.decompress(value, orig_data_type, value_count);

        logger.log(Level.WARNING, "JPEG decoding is only supported for original data types byte and ubyte, not " + orig_data_type);
        return value;
    }

    //private static int updates = 0;
    //private static long avg_ns = 0;

    @Override
    public byte[] decompress(final byte[] data, final int decompressed_size) throws Exception
    {
        // Dump raw JPG file to debug
        // try (FileOutputStream out = new FileOutputStream("/tmp/data.jpg"))
        // {  out.write(data); }

        final ByteArrayInputStream in = new ByteArrayInputStream(data);

//        long start = System.nanoTime();
        final byte[] result;

//        // Expand using JavaFX Image
//        // - Adds UI dependency to what's otherwise a non-UI module
//        // - With 1024 x 1024 pixel sim detector image, this took about 13..20 ms
//        final Image image = new Image(in);
//        final int width = (int) image.getWidth();
//        final int height = (int) image.getHeight();
//        if (width * height != decompressed_size)
//            throw new Exception("Expected JPEG with size " + decompressed_size + " but got " +
//                                width + " x " + height + " = " + (width * height) + " bytes");
//
//        result = new byte[decompressed_size];
//        final PixelReader pixels = image.getPixelReader();
//        for (int y=0; y<height; ++y)
//            for (int x=0; x<width; ++x)
//            {
//                final int rgba = pixels.getArgb(x, y);
//                // Area detector only uses lower 8 bits
//                result[x+y*width] = (byte) (rgba & 0xFF);
//            }

        // Expand using AWT API
        // + Is strictly speaking UI and also old API, but no dependency beyond standard JRE
        // + With 1024 x 1024 pixel sim detector image, this took about 2.5 .. 3.0 ms
        final BufferedImage image = ImageIO.read(in);
        final int width = image.getWidth();
        final int height = image.getHeight();
        if (width * height != decompressed_size)
            throw new Exception("Expected JPEG with size " + decompressed_size + " but got " +
                                width + " x " + height + " = " + (width * height) + " bytes");

        final int type = image.getType();
        if (type == BufferedImage.TYPE_BYTE_GRAY)
        {
            final DataBufferByte buf = (DataBufferByte) image.getData().getDataBuffer();
            result = buf.getData();
            if (result.length != decompressed_size)
                logger.log(Level.WARNING,
                           "Expected " + decompressed_size + " expanded JPEG bytes but got " +
                           result.length);
        }
        else
            throw new Exception("Expected TYPE_BYTE_GRAY but got type code " + type);

//        long ns = System.nanoTime() - start;
//        avg_ns = (avg_ns + ns)/2;
//        if (++updates > 10)
//        {
//            updates = 0;
//            System.out.println(avg_ns/1e6 + " ms");
//        }

        return result;
    }
}
