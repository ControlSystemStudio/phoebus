/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import org.epics.vtype.Display;
import org.epics.vtype.VType;

/** Formatter for {@link VType} values that uses {@link Double#toString()}
 *  @author Kay Kasemir
 */
public class DoubleVTypeFormat extends VTypeFormat
{
    final private static VTypeFormat instance = new DoubleVTypeFormat();

    final public static VTypeFormat get()
    {
        return instance;
    }

    @Override
    public StringBuilder format(final Number number,
            final Display display, final StringBuilder buf)
    {
        if (number instanceof Double)
        {
            final Double dbl = (Double) number;
            if (dbl.isNaN())
                return buf.append(VTypeFormat.NOT_A_NUMBER);
            else if (dbl.isInfinite())
                return buf.append(VTypeFormat.INFINITE);
            return buf.append(Double.toString(dbl));
        }
        return buf.append(number.toString());
    }
}
