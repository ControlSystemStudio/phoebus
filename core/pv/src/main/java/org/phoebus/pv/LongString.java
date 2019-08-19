/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv;

import java.nio.charset.Charset;

import org.epics.util.array.ListNumber;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VNumberArray;

/** Support for EPICS 'long string'
 *
 *  <p>In the EPICS database and Channel Access,
 *  DBF_STRING refers to text limited to 40 characters.
 *  Larger text is often transferred as DBF_CHAR[].
 *
 *  <p>This helper decodes a string from a char/byte array.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LongString
{
    /** [85, 84, 70, 45, 56] */
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /** @param value Array of numbers. Typically {@link VByteArray}.
     *  @return String based on character for each array element
     */
    public static String fromArray(final VNumberArray value)
    {
        final ListNumber data = value.getData();
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
        // Use actual 'len', not data.size()
        return new String(bytes, 0, len, UTF8);
    }
}
