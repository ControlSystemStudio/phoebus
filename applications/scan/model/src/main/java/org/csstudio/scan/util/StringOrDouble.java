/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.util;

/** Helper for handling {@link Object}s that are
 *  either a {@link String} or a {@link Double}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StringOrDouble
{
    /** @param value Value that's either String or a number
     *  @return Quoted string, or the text representation of the number
     */
    public static String quote(final Object value)
    {
        if (value instanceof String)
            return '"' + (String) value + '"';
        return value.toString();
    }

    /** @param text Text that contains quoted string or a number
     *  @return {@link String} or {@link Double}
     */
    public static Object parse(String text)
    {
        text = text.trim();
        if (text.isEmpty())
            return Double.valueOf(0);
        if (text.startsWith("\""))
        {
            if (text.endsWith("\""))
                return text.substring(1, text.length()-1);
            // Only starting quote: Still assume it's a string
            return text.substring(1, text.length());
        }
        try
        {
            return Double.parseDouble(text);
        }
        catch (NumberFormatException ex)
        {   // Use String
            return text;
        }
    }
}
