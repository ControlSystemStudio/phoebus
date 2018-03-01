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
     *  @throws NumberFormatException if number cannot be parsed, so needs to be handled as {@link String}
     */
    public static Object parse(String text) throws NumberFormatException
    {
        text = text.trim();
        if (text.startsWith("\""))
        {
            if (text.endsWith("\""))
                return text.substring(1, text.length()-1);
            // Only starting quote: Still assume it's a string
            return text.substring(1, text.length());
        }
        return Double.parseDouble(text);
    }
}
