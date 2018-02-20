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

/** Factory for {@link ScanSample} instances
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanSampleFactory
{
    /** Create ScanSample for plain number
     *  @param timestamp Time stamp
     *  @param serial Serial to identify when the sample was taken
     *  @param numbers {@link Number}s
     *  @return {@link ScanSample}
     *  @throws IllegalArgumentException if the value type is not handled
     */
    public static ScanSample createSample(final Instant timestamp,
            final long serial, final Number... numbers) throws IllegalArgumentException
    {
        if (numbers.length <= 0)
            throw new IllegalArgumentException("Missing values");
        return new NumberScanSample(timestamp, serial, numbers);
    }

    /** Create ScanSample for strings
     *  @param timestamp Time stamp
     *  @param serial Serial to identify when the sample was taken
     *  @param values {@link String}s
     *  @return {@link ScanSample}
     *  @throws IllegalArgumentException if the value type is not handled
     */
    public static ScanSample createSample(final Instant timestamp,
            final long serial, final String... values) throws IllegalArgumentException
    {
        if (values.length <= 0)
            throw new IllegalArgumentException("Missing values");
        return new StringScanSample(timestamp, serial, values);
    }

    /** Create ScanSample for plain number or text value
     *  @param timestamp Time stamp
     *  @param serial Serial to identify when the sample was taken
     *  @param values Values, must all have the same type
     *  @return {@link ScanSample}
     *  @throws IllegalArgumentException if the value type is not handled
     */
    public static ScanSample createSample(final Instant timestamp,
            final long serial, final Object... values) throws IllegalArgumentException
    {
        if (values.length <= 0)
            throw new IllegalArgumentException("Missing values");
        if (values instanceof Number[])
            return new NumberScanSample(timestamp, serial, (Number[]) values);
        else if (values instanceof String[])
            return new StringScanSample(timestamp, serial, (String[]) values);
        else if (values.length == 1  &&  values[0] instanceof Double)
            return new NumberScanSample(timestamp, serial, (Double) values[0]);
        throw new IllegalArgumentException("Cannot handle values of type " + values[0].getClass().getName());
    }
}
