/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import org.epics.util.array.ArrayByte;
import org.epics.util.array.ListNumber;

/** PVA NDArray codec for uncompressed data
 *  @author Kay Kasemir
 */
public class UncompressedCodec implements Codec
{
    @Override
    public ListNumber decode(final byte[] data, final int decompressed_size) throws Exception
    {
        return ArrayByte.of(data);
    }
}
