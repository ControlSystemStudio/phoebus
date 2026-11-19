/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.util;

/** Helper for handling {@link Object}s that are
 *  a {@link String}, a {@link Double}, or a {@link PVReference}.
 *
 *  <p>Parsing rules:
 *  <ul>
 *    <li>Quoted text (e.g. {@code "hello"}) &rarr; {@link String} (literal text comparison)</li>
 *    <li>A number (e.g. {@code 5.3}) &rarr; {@link Double} (numeric comparison)</li>
 *    <li>Unquoted non-numeric text (e.g. {@code some:pv:name}) &rarr; {@link PVReference}
 *        (read that PV at runtime and use its value for comparison)</li>
 *  </ul>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StringOrDouble
{
    /** @param value Value that's a {@link String}, {@link Double}, or {@link PVReference}
     *  @return Quoted string for literals, or the text representation for numbers/PV references
     */
    public static String quote(final Object value)
    {
        if (value instanceof String)
            return '"' + (String) value + '"';
        // PVReference and Double both render as plain text (no quotes)
        return value.toString();
    }

    /** @param text Text that contains a quoted string, a number, or a bare PV name
     *  @return {@link String} for quoted literals, {@link Double} for numbers,
     *          {@link PVReference} for unquoted non-numeric text
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
            // Only starting quote: Still assume it's a string literal
            return text.substring(1, text.length());
        }
        try
        {
            return Double.parseDouble(text);
        }
        catch (NumberFormatException ex)
        {   // Unquoted non-numeric text: treat as a PV name reference
            return new PVReference(text);
        }
    }
}
