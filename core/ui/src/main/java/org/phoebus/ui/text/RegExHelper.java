/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.text;

/** Helper for creating regular expression from 'glob' pattern
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RegExHelper
{
    /** Convert a file-glob type pattern with '?' and '*'
     *  into a regular expression.
     *  <ul>
     *  <li>'?' can be used to match single character
     *  <li>'*' can be used to zero or more characters
     *  </ul>
     *  The result is not 'anchored', it does not start
     *  with '\A' or '.*', to support code that looks for
     *  a regular expression sub-match.
     *  @param pattern File-glob
     *  @return Regular expression string.
     */
    public static String regexFromGlob(String pattern)
    {
        // Simplify: Reg ex won't be 'anchored', so remove
        // leading and trailing stuff
        while (pattern.startsWith("*"))
            pattern = pattern.substring(1);
        while (pattern.endsWith("*"))
            pattern = pattern.substring(0, pattern.length()-1);

        // Mask stuff that's recognized by a regular expression:
        // '.'
        pattern = pattern.replace(".", "\\.");

        // Replace glob ? by reg ex .
        pattern = pattern.replace('?', '.');

        // Replace glob * by reg ex .*
        pattern = pattern.replace("*", ".*");
        return pattern;
    }

    /** Convert a file-glob type pattern with '?' and '*'
     *  into a regular expression.
     *  <ul>
     *  <li>'?' can be used to match single character
     *  <li>'*' can be used to zero or more characters
     *  </ul>
     *  The result starts and ends with '.*' to support
     *  the Java Matcher which always matches the full string.
     *  @param pattern File-glob
     *  @return Regular expression string.
     */
    public static String fullRegexFromGlob(final String pattern)
    {
        return ".*" + regexFromGlob(pattern) + ".*";
    }
}
