/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.vtype;

/** Sexagesimal format
 *
 *  @author Kay Kasemir
 *  @author lcavalli provided original implementation for BOY, https://github.com/ControlSystemStudio/cs-studio/pull/1978
 */
@SuppressWarnings("nls")
public class SexagesimalFormat
{
    private static final double[] prec_tab = new double[]
    {
        1.0, 1.0 / 6.0, 1.0 / 60.0, 1.0 / 360.0, 1.0 / 3.6E3,
        1.0 / 3.6E4, 1.0 / 3.6E5, 1.0 / 3.6E6, 1.0 / 3.6E7
    };

    private static final int MAXPREC = prec_tab.length + 1;

    /** Format number as sexagesimal hours:minutes:seconds with fractional seconds
     *
     *  <p>Precision determines the number of digits used for
     *  minutes, seconds, fractional seconds.
     *  For example, "12:34:56.789" has a precision of 7.
     *  With a precision of 2 it would become "12:35",
     *  and with a precision of 4 "12:34:57".
     *
     *  @param value Number to format
     *  @param precision Digits used for minutes, seconds, fractional seconds
     *  @return "HH:MM:SS.SSS" type text
     */
    public static String format(double value, int precision)
    {
        double prec_frac, frac;

        // Round the multiplier required to represent the value as an integer,
        //   retaining the required precision
        if (precision <= MAXPREC)
            prec_frac = prec_tab[precision];
        else {
            prec_frac = prec_tab[MAXPREC];
            for (int i = precision; i > MAXPREC; i--)
                prec_frac *= 0.1;
        }

        // Add half the maximum displayed precision to aid with rounding
        value = value + 0.5 * prec_frac;

        final StringBuilder builder = new StringBuilder();

        // Insert a leading negative sign, if required
        if(value < 0.0)
        {
            builder.append('-');
            value = -value + prec_frac;
        }

        /* Now format the numbers */
        final double hrs = Math.floor(value);
        value = (value - hrs) * 60.0;
        final int min = (int) value;
        value = (value - min) * 60.0;
        final int sec = (int) value;

        if (precision == 0)
            builder.append(String.format("%.0f", hrs));
        else if (precision == 1)
            builder.append(String.format("%.0f:%d", hrs, (min / 10)));
        else if (precision == 2)
            builder.append(String.format("%.0f:%02d", hrs, min));
        else if (precision == 3)
            builder.append(String.format("%.0f:%02d:%d", hrs, min, sec / 10));
        else if (precision == 4)
            builder.append(String.format("%.0f:%02d:%02d", hrs, min, sec));
        else
        {
            frac = Math.floor((value - sec) / (prec_frac * 3600.0));
            builder.append(String.format("%.0f:%02d:%02d.%0" + (precision - 4) + ".0f", hrs, min, sec, frac));
        }

        return builder.toString();
    }

    private static double parseDouble(final String text) throws NumberFormatException
    {
        return Double.valueOf(text.replace('e', 'E'));
    }

    /** Parse sexagesimal text
     *
     *  @param text Text of format "HH:MM:SS.SSS"
     *  @return Number
     *  @throws Exception on error
     */
    public static double parse(final String text) throws Exception
    {
        final String[] parts = text.trim().split(":");

        if (parts.length <= 0)
            throw new Exception("Missing ':' in sexagesimal '" + text + "'");

        final boolean negative = parts[0].startsWith("-");
        if (negative)
            parts[0] = parts[0].replace("-", "");

        // Hours are always present
        double value;
        try
        {
            value = parseDouble(parts[0]);
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Missing hours in sexagesimal '" + text + "'", ex);
        }

        if (value != Math.floor(value)  ||  value < 0.0)
            throw new Exception("Invalid hours in sexagesimal '" + text + "'");

        if (parts.length > 1)
        {   // Minutes are present
            double minutes;
            try
            {
                minutes = parseDouble(parts[1]);
            }
            catch (NumberFormatException ex)
            {
                throw new Exception("Cannot parse minutes in sexagesimal '" + text + "'", ex);
            }
            if (minutes != Math.floor(minutes)  ||  minutes < 0.0)
                throw new Exception("Invalid minutes in sexagesimal '" + text + "'");

            value += minutes / 60.0;

            if (parts.length > 2)
            {   // Seconds are present
                double seconds;
                try
                {
                    seconds = parseDouble(parts[2]);
                }
                catch (NumberFormatException ex)
                {
                    throw new Exception("Cannot parse seconds in sexagesimal '" + text + "'", ex);
                }
                if (seconds < 0)
                    throw new Exception("Invalid seconds in sexagesimal '" + text + "'");
                value += seconds / 3600.0;
            }
        }

        // Apply original sign
        if (negative)
            return -value;
        return value;
    }
}
