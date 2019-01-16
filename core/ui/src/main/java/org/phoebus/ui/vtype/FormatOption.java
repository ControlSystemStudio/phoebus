/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.vtype;

import org.phoebus.ui.Messages;

/** Options for formatting a value
 *  @author Kay Kasemir
 */
public enum FormatOption
{
    /** Use default settings from PV */
    DEFAULT(Messages.Format_Default, false),

    /** Use decimal representation, precision determines number of decimals */
    DECIMAL(Messages.Format_Decimal, true),

    /** Use exponential representation, precision determines number of decimals */
    EXPONENTIAL(Messages.Format_Exponential, true),

    /** Use exponential representation where exponent is multiple of 3, precision determines number of decimals */
    ENGINEERING(Messages.Format_Engineering, true),

    /** Use hexadecimal representation, precision determines number of hex digits. 8 for 32 bits */
    HEX(Messages.Format_Hexadecimal, true),

    /** Decimal for values in 0.0001 &lt;= |value| &lt;= 10000, else exponential, precision determines number of of decimals */
    COMPACT(Messages.Format_Compact, true),

    /** Force string, most important for array-of-bytes */
    STRING(Messages.Format_String, false),

    /** Sexagesimal degrees-or-hours:minutes:seconds */
    SEXAGESIMAL(Messages.Format_Sexagesimal, false),

    /** Sexagesimal, number is assumed to be radians with 2pi == 24 hours */
    SEXAGESIMAL_HMS(Messages.Format_SexagesimalHMS, false),

    /** Sexagesimal, number is assumed to be radians with 2pi == 360 degrees */
    SEXAGESIMAL_DMS(Messages.Format_SexagesimalDMS, false),

    // Binary was added when PVA introduced it.
    /** Binary, precision determines the number of 01010101 */
    BINARY(Messages.Format_Binary, true);

    // To remain compatible with previous versions of this enum,
    // new options must be added to the end.

    private final String label;
    private final boolean use_precision;

    private FormatOption(final String label, final boolean use_precision)
    {
        this.label = label;
        this.use_precision = use_precision;
    }

    public boolean isUsingPrecision()
    {
        return use_precision;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
