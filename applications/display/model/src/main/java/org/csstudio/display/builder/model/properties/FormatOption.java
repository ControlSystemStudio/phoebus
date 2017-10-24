/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import org.csstudio.display.builder.model.Messages;

/** Options for formatting a value
 *  @author Kay Kasemir
 */
public enum FormatOption
{
    /** Use default settings from PV */
    DEFAULT(Messages.Format_Default),

    /** Use decimal representation, precision determines number of decimals */
    DECIMAL(Messages.Format_Decimal),

    /** Use exponential representation, precision determines number of decimals */
    EXPONENTIAL(Messages.Format_Exponential),

    /** Use exponential representation where exponent is multiple of 3, precision determines number of decimals */
    ENGINEERING(Messages.Format_Engineering),

    /** Use hexadecimal representation, precision determines number of hex digits. 8 for 32 bits */
    HEX(Messages.Format_Hexadecimal),

    /** Decimal for values in 0.0001 <= |value| <= 10000, else exponential, precision determines number of of decimals */
    COMPACT(Messages.Format_Compact),

    /** Force string, most important for array-of-bytes */
    STRING(Messages.Format_String),

    /** Sexagesimal degrees-or-hours:minutes:seconds */
    SEXAGESIMAL(Messages.Format_Sexagesimal),

    /** Sexagesimal, number is assumed to be radians with 2pi == 24 hours */
    SEXAGESIMAL_HMS(Messages.Format_SexagesimalHMS),

    /** Sexagesimal, number is assumed to be radians with 2pi == 360 degrees */
    SEXAGESIMAL_DMS(Messages.Format_SexagesimalDMS);

    private final String label;

    private FormatOption(final String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
