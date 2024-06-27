/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import java.text.NumberFormat;

import org.epics.vtype.Display;
import org.epics.vtype.VEnum;
import org.epics.vtype.VType;

/** Formatter for {@link VType} values that uses the default {@link Display} info
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DecimalVTypeFormat extends VTypeFormat
{
    final protected int precision;
    final private NumberFormat format;

    /** Initialize
     *  @param precision Desired number of decimal digits
     */
    public DecimalVTypeFormat(final int precision)
    {
        this.precision = precision;
        this.format = initFormat();
    }

    /** @return NumberFormat to use in this formatter */
    protected NumberFormat initFormat()
    {
        final NumberFormat format = NumberFormat.getNumberInstance();
        format.setMinimumFractionDigits(precision);
        format.setMaximumFractionDigits(precision);
        return format;
    }

    /** {@inheritDoc} */
    @Override
    public StringBuilder format(final VType value, final StringBuilder buf)
    {
        if (value instanceof VEnum)
        {
            final VEnum enumerated = (VEnum)value;
            format(enumerated.getIndex(), null, buf);
        }
        else
            super.format(value, buf);
        return buf;
    }

    /** {@inheritDoc} */
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
        }
        return buf.append(format.format(number));
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "Decimal (" + precision + " digits)";
    }
}
