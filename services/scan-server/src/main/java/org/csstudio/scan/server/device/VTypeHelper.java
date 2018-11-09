/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.device;

import java.time.Instant;

import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.data.ScanSampleFactory;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VType;

/** Helper for handling {@link VType} data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypeHelper
{
    /** Format value as string
     *  @param value {@link VType}
     *  @return String representation
     */
    final public static String toString(final VType value)
    {
        if (value instanceof VNumber)
            return ((VNumber)value).getValue().toString();
        if (value instanceof VEnum)
        {
            try
            {
                return ((VEnum)value).getValue();
            }
            catch (ArrayIndexOutOfBoundsException ex)
            {    // PVManager doesn't handle enums that have no label
                return "<enum " + ((VEnum)value).getIndex() + ">";
            }
        }
        if (value instanceof VString)
            return ((VString)value).getValue();
        if (value == null)
            return "null";
        return value.toString();
    }

    /** Get VType as double or NaN if not possible
     *  @param value {@link VType}
     *  @return double or NaN
     */
    final public static double toDouble(final VType value)
    {
        if (value instanceof VNumber)
            return ((VNumber)value).getValue().doubleValue();
        if (value instanceof VEnum)
            return ((VEnum)value).getIndex();
        if (value instanceof VString)
        {
            try
            {
                return Double.parseDouble(((VString) value).getValue());
            }
            catch (NumberFormatException ex)
            {
                // Ignore
            }
        }
        return Double.NaN;
    }

    /** Get VType as double[]; empty array if not possible
     *  @param value {@link VType}
     *  @return double[]
     */
    public static double[] toDoubles(final VType value)
    {
        final double[] array;
        if (value instanceof VNumberArray)
        {
            final ListNumber list = ((VNumberArray) value).getData();
            array = new double[list.size()];
            for (int i=0; i<array.length; ++i)
                array[i] = list.getDouble(i);
        }
        else
            array = new double[0];
        return array;
    }

    /** Create ScanSample for control system value
     *  @param serial Serial to identify when the sample was taken
     *  @param value {@link VType}
     *  @return {@link ScanSample}
     *  @throws IllegalArgumentException if the value type is not handled
     */
    public static ScanSample createSample(final long serial, final VType value) throws IllegalArgumentException
    {
        final Instant date = Time.timeOf(value).getTimestamp();
        // Log anything numeric as NumberSample
        if (value instanceof VNumber)
            return ScanSampleFactory.createSample(date, serial, ((VNumber) value).getValue());
        else if (value instanceof VString)
            return ScanSampleFactory.createSample(date, serial, ((VString) value).getValue());
        else if (value instanceof VNumberArray)
        {   // Handle any numeric array as such
            final ListNumber list = ((VNumberArray) value).getData();
            final Object[] numbers = new Number[list.size()];
            for (int i=0; i<numbers.length; ++i)
                numbers[i] = list.getDouble(i);
            return ScanSampleFactory.createSample(date, serial, numbers);
        }
        else
            return ScanSampleFactory.createSample(date, serial, toString(value));
    }
}
