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
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;

import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream;
import org.epics.util.array.ArrayByte;
import org.epics.util.array.ListNumber;

/** PVA NDArray codec for LZ4-compressed data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LZ4Codec implements Codec
{
    @Override
    public ListNumber decode(final byte[] data, final int decompressed_size) throws Exception
    {
        final BlockLZ4CompressorInputStream in =
            new BlockLZ4CompressorInputStream(
                new ByteArrayInputStream(data));
        final ByteArrayOutputStream out = new ByteArrayOutputStream(decompressed_size);
        final byte[] buffer = new byte[1024];

        int got = 0;
        int n;
        while ((n = in.read(buffer)) != -1)
        {
            got += n;
            out.write(buffer, 0, n);
        }
        out.close();
        in.close();

        if (got != decompressed_size)
            throw new Exception("LZ4 decompression resulted in " + got +
                                " instead of exepected " + decompressed_size + " bytes");

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "LZ4 expands " + got + " into " + decompressed_size + " bytes");

        return ArrayByte.of(out.toByteArray());
    }
}
