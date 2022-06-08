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
        final byte[] result = new byte[decompressed_size];
        int expanded = 0;

        try
        ( final BlockLZ4CompressorInputStream in =
            new BlockLZ4CompressorInputStream(new ByteArrayInputStream(data))
        )
        {
            while (expanded < decompressed_size)
            {
                final int batch = in.read(result, expanded, decompressed_size - expanded);
                if (batch == -1)
                    break;
                expanded += batch;
            }
        }

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "LZ4 expands " + expanded + " into " + decompressed_size + " bytes");

        return ArrayByte.of(result);
    }
}
