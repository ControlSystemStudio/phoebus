/*******************************************************************************
 * Copyright (c) 2026 Canadian Light Source Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.vtype;

import org.phoebus.ui.Messages;

/** Formatting options for numeric scale/axis labels.
 *
 *  <p>A subset of {@link FormatOption} containing only the formats that
 *  are meaningful on a numeric scale axis (Tank, Thermometer, ProgressBar,
 *  Meter, etc.).  Text-only formats like STRING, HEX, BINARY and
 *  SEXAGESIMAL are excluded because they have no sensible rendering on
 *  an axis.
 *
 *  <p>The ordinal order is independent of {@link FormatOption} and
 *  places the most useful options first.
 *
 *  @author Heredie Delvalle &mdash; CLS
 */
public enum ScaleFormat
{
    /** Automatic formatting chosen by the axis tick algorithm */
    DEFAULT(Messages.Format_Default),

    /** Significant-digits formatting (like C/Java {@code %g}).
     *  Precision controls total significant digits, not fraction digits. */
    SIGNIFICANT(Messages.Format_Significant),

    /** Fixed decimal places */
    DECIMAL(Messages.Format_Decimal),

    /** Scientific notation (e.g. 1.23E4) */
    EXPONENTIAL(Messages.Format_Exponential),

    /** Engineering notation (exponent is a multiple of 3) */
    ENGINEERING(Messages.Format_Engineering),

    /** Decimal when in range 0.0001..10000, else exponential */
    COMPACT(Messages.Format_Compact);

    // New values must be appended at the end to preserve ordinal
    // compatibility with serialized .bob files.

    private final String label;

    ScaleFormat(final String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
