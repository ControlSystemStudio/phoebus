/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.data;

import java.time.Instant;
import java.util.Arrays;

/** Scan sample for strings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StringScanSample extends ScanSample
{
    final private String[] values;

    /** Initialize
     *  @param timestamp Time stamp
     *  @param serial Serial to identify when the sample was taken
     *  @param values Values
     */
    public StringScanSample(final Instant timestamp,
            final long serial, final String... values)
    {
        super(timestamp, serial);
        this.values = values;
    }

    /** @return Array size */
    public int size()
    {
        return values.length;
    }

    /** @param index Array index
     *  @return String for that array index
     */
    public String getString(final int index)
    {
        return values[index];
    }

    /** {@inheritDoc} */
    @Override
    public Object[] getValues()
    {
        return values;
    }

    @Override
    public String toString()
    {
        if (size() == 1)
            return super.toString() + " " + values[0].toString();
        return super.toString() + " " + Arrays.toString(values);
    }
}
