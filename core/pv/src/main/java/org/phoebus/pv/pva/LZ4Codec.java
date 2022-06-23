/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import java.io.ByteArrayInputStream;

import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream;

/** PVA NDArray codec for LZ4-compressed data
 *
 *  <p>Standard Java library only supports ZIP.
 *  Area detector provides binaries/JNI interface
 *  for LZ4 and BLOSC, but that would add a hard to maintain
 *  dependency on binaries.
 *  LZ4 is available via apache-commons-compress, making
 *  it easy to support.
 *
 *  @author Kay Kasemir
 */
public class LZ4Codec extends Codec
{
    @Override
    public byte[] decompress(final byte[] data, final int decompressed_size) throws Exception
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

        return result;
    }
}
