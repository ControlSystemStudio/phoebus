/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import org.epics.util.array.ListNumber;

/** PVA NDArray compression codec
 *  @author Kay Kasemir
 */
public interface Codec
{
    /** De-compress byte array
     *
     *  @param data Compressed data
     *  @param decompressed_size Expected de-compressed size in bytes
     *  @return Decompressed data
     *  @throws Exception on error
     */
    public ListNumber decode(byte[] data, final int decompressed_size) throws Exception;
}
