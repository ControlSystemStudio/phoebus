/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import static org.phoebus.pv.PV.logger;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;

import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVAData;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

/** PVA NDArray codec for JPEG-compressed data
 *
 *  <p>Standard Java library only supports ZIP.
 *  Area detector provides binaries/JNI interface
 *  for JPEG, but that would add a hard to maintain
 *  dependency on binaries.
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

    @Override
    public byte[] decompress(final byte[] data, final int decompressed_size) throws Exception
    {
        // Dump raw JPG file to debug
        // try (FileOutputStream out = new FileOutputStream("/tmp/data.jpg"))
        // {  out.write(data); }

        final ByteArrayInputStream in = new ByteArrayInputStream(data);
        final Image image = new Image(in);
        final int width = (int) image.getWidth();
        final int height = (int) image.getHeight();
        if (width * height != decompressed_size)
        {
            logger.log(Level.WARNING,
                       "Expected JPEG with size " + decompressed_size + " but got " +
                       width + " x " + height + " = " + (width * height) + " bytes");
            return data;
        }

        final PixelReader pixels = image.getPixelReader();
        final byte[] result = new byte[decompressed_size];
        for (int y=0; y<height; ++y)
            for (int x=0; x<width; ++x)
            {
                final int rgba = pixels.getArgb(x, y);
                // Area detector only uses lower 8 bits
                result[x+y*width] = (byte) (rgba & 0xFF);
            }

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "JPEG expands " + data.length + " into " + decompressed_size + " bytes");

        return result;
    }
}
