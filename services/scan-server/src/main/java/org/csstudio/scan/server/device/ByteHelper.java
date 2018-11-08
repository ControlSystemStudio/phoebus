/*******************************************************************************
 * Copyright (c) 2013-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.device;

import org.epics.util.array.ListByte;
import org.epics.vtype.VByteArray;

/** Helper for handling byte arrays
 *  @author Kay Kasemir
 */
public class ByteHelper
{
    /** Convert byte array into String
     *  @param barray {@link VByteArray}
     *  @return {@link String}
     */
    final public static String toString(final VByteArray barray)
    {
        final ListByte data = barray.getData();
        final byte[] bytes = new byte[data.size()];
        // Copy bytes until end or '\0'
        int len = 0;
        while (len<bytes.length)
        {
            final byte b = data.getByte(len);
            if (b == 0)
                break;
            else
                bytes[len++] = b;
        }
        return new String(bytes, 0, len);
    }

    /** Convert String into byte array
     *  @param text {@link String}
     *  @return byte array, always including '\0' termination
     */
    final public static byte[] toBytes(final String text)
    {   // Write string as byte array WITH '\0' TERMINATION!
        final byte[] bytes = new byte[text.length() + 1];
        System.arraycopy(text.getBytes(), 0, bytes, 0, text.length());
        bytes[text.length()] = '\0';
        return bytes;
    }
}
