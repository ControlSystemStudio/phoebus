/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.device;

import java.time.Instant;

import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.data.ScanSampleFactory;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Time;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

/** Helper for handling {@link VType} data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanSampleHelper
{

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
            return ScanSampleFactory.createSample(date, serial, VTypeHelper.toString(value));
    }
}
