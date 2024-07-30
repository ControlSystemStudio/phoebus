/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import java.util.ArrayList;
import java.util.List;

/** Parser for dropped PV names
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DroppedPVNameParser
{
    /** Parse PV names from text
     *
     *  <p>Text may contain
     *  <ul>
     *  <li>Just one PV name, which may be "sim://sine(1, 10, 0.1)"
     *  <li>List of PV names separated by newlines
     *  <li>.. separated by space, comma, semicolon
     *  </ul>
     *
     *  @param text Text that may contain PV names
     *  @return List of PV names
     *  @throws Exception on error
     */
    public static List<String> parseDroppedPVs(String text) throws Exception
    {
        text = text.trim();
        // Remove optional 'array' wrapper
        if (text.startsWith("[")  &&  text.endsWith("]"))
            text = text.substring(1, text.length()-2);

        final List<String> names = new ArrayList<>();

        // Need to look for a separator, but skipping them inside quoted text and brackets
        final int len = text.length();
        int start = 0, pos = 0;

        while (pos < len)
        {
            final char c = text.charAt(pos);
            if (c == '"')
                pos = locateClosingQuote(text, pos+1);
            else if (c == '(')
                pos = locateClosingBrace(text, pos+1);
            else if ("\r\n\t,; ".indexOf(c) >= 0)
            {   // Found one of the separators
                final String name = text.substring(start, pos).trim();
                if (! name.isEmpty())
                    names.add(name);
                start = ++pos;
            }
            else
                ++pos;
        }
        // Was there a last name?
        if (pos > start)
        {
            final String name = text.substring(start, pos).trim();
            if (! name.isEmpty())
                names.add(name);
        }
        return names;
    }

    /** Locate closing, non-escaped quote
     *  @param text Text to search
     *  @param pos Position after opening quote
     *  @return Position after the closing quote, or -1
     *  @throws Exception if there is no closing quote
     */
    private static int locateClosingQuote(final String text, int pos) throws Exception
    {
        int end = text.indexOf('"', pos);
        while (end > pos && text.charAt(end-1) == '\\')
            end = text.indexOf('"', end+1);
        if (end < 0)
            throw new Exception("Missing closing quote");
        return end + 1;
    }

    /** Locate closing brace, ignoring those inside quotes
     *  @param text Text to search
     *  @param pos Position after opening quote
     *  @return Position after the closing quote, or -1
     *  @throws Exception if there is no closing quote
     */
    private static int locateClosingBrace(final String text, int pos) throws Exception
    {
        final int end = text.length();
        while (pos < end)
        {
            final char c = text.charAt(pos);
            if (c == '"')
                pos = locateClosingQuote(text, pos+1);
            else if (c == ')')
                return pos;
            else
                ++pos;
        }
        throw new Exception("Missing closing brace");
    }
}
