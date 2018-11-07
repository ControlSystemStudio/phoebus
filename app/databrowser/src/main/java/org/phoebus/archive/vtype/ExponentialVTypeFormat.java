/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.epics.vtype.VType;

/** Formatter for {@link VType} values that uses exponential formatting
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExponentialVTypeFormat extends DecimalVTypeFormat
{
    /** Initialize
     *  @param precision Desired number of decimal digits
     */
    public ExponentialVTypeFormat(final int precision)
    {
        super(precision);
    }

    /** {@inheritDoc} */
    @Override
    protected NumberFormat initFormat()
    {
        // Is there a better way to get this silly format?
        final StringBuffer pattern = new StringBuffer(10);
        pattern.append("0.");
        for (int i=0; i<precision; ++i)
            pattern.append('0');
        pattern.append("E0");
        return new DecimalFormat(pattern.toString());
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "Exponential (" + precision + " digits)";
    }
}
