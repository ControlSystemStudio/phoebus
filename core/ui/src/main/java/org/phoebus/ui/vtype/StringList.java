/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.vtype;

import java.util.ArrayList;
import java.util.List;

/** Utility for handling array of Strings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StringList
{
    /** Join items into a quoted, comma-separated string
     *  @param items Several string items
     *  @return One string
     */
    public static String join(final List<String> items)
    {
        if (items.isEmpty())
            return "[]";
        final StringBuilder buf = new StringBuilder("[");
        buf.append('"').append(escape(items.get(0))).append('"');
        for (int i=1; i<items.size(); ++i)
            buf.append(", \"").append(escape(items.get(i))).append('"');
        buf.append("]");
        return buf.toString();
    }

    /** @param string String
     *  @return String where quotes are escaped
     */
    private static String escape(String string)
    {
        return string.replace("\"", "\\\"");
    }

    /** Split text with comma-separated, quoted items
     *  @param text Text with items
     *  @return List of items
     *  @throws Exception on error
     */
    public static List<String> split(String text) throws Exception
    {
        text = text.trim();
        // Remove array brackets
        if (text.startsWith("["))
            text = text.substring(1);
        if (text.endsWith("]"))
            text = text.substring(0, text.length()-1);
        final List<String> items = new ArrayList<>();
        int pos = 0;
        while (pos < text.length())
        {
            final char c = text.charAt(pos);
            // Skip space
            if (c == ' '  ||  c == '\t')
                ++pos;

            // Handle quoted string
            else if (c == '"')
            {   // Locate closing, non-escaped quote
                int end = text.indexOf('"', pos+1);
                while (end > pos)
                {
                    if (text.charAt(end-1) == '\\')
                    {   // Quote is escaped. Remove the escape, search on
                        text = text.substring(0, end-1) + text.substring(end);
                        end = text.indexOf('"', end);
                    }
                    else
                        break;
                }
                if (end < 0)
                    throw new Exception("Missing closing quote");
                // Use string without quotes
                items.add(text.substring(pos+1, end));
                pos = end + 1;
                // Advance to comma at end of string
                while (pos < text.length() && text.charAt(pos) != ',')
                    ++pos;
                ++pos;
            }

            // Handle unquoted item
            else
            {   // Locate comma
                int end = pos+1;
                while (end < text.length()  &&
                       text.charAt(end) != ',')
                    ++end;
                items.add(text.substring(pos, end).trim());
                pos = end+1;
            }
        }
        return items;
    }
}
