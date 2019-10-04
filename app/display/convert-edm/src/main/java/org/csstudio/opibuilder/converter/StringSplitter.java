/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter;

import java.util.regex.Pattern;

/**
 * NOTE: this class has been copied from the org.csstudio.java plug-in in
 * order to allow compiling this plug-in into a stand-alone jar file.
 * Please edit that file and copy here if changes need to be made.
 *
 * Split string into segments
 *
 *  @author Nick Battam
 *  @author Kay Kasemir
 *  @author Xihui Chen - Original <code> StringUtil.splitIgnoreInQuotes()</code>
 *
 */
public class StringSplitter
{
    private static final String QUOTE = "'\\\"";
    private static final String NOT_QUOTE = "^" + QUOTE;

    private static final char SPACE = ' ';
    private static final char TAB = '\t';
    private static final char PIPE = '|';

    private static final String ESCAPED_QUOTE = "\\\\\\\"";
    private static final String ESCAPED_SINGLE_QUOTE = "\\\\\'";

    static final String SUBSTITUTE_QUOTE = "\uF8FF";
    static final String SUBSTITUTE_SINGLE_QUOTE = "\uE000";

    private static final String splitRegex = "(?="
            + "([" + NOT_QUOTE + "]*"  // any number of non-quotes
            +  "[" + QUOTE + "]"       // a quote
            +  "[" + NOT_QUOTE + "]*"  // any number of non-quotes
            +  "[" + QUOTE + "]"       // a quote, not preceeded by an escape
            + ")*"                     // any number of times
            +  "[" + NOT_QUOTE + "]*"  // any number of non quotes
            + "$)";

    /** Prevent instantiation */
    private StringSplitter()
    {
        // NOP
    }


    /** Split source string into an array of elements separated by the splitting character,
     *  but ignoring split characters enclosed in quotes.
     *
     *  @param trimmedSource String to be split
     *  @param splitChar Character used to split the source string, e.g. ',' or ' '
     *  @param deleteHeadTailQuotes Delete quotes in the head and tail of individual elements
     *                              if <code>true</code>
     *  @return Array of individual elements
     *  @throws Exception on parse error (missing end of quoted string)
     */
    @SuppressWarnings("nls")
    public static String[] splitIgnoreInQuotes(final String source,
                                               final char splitChar,
                                               final boolean deleteHeadTailQuotes) throws Exception
    {
        // Trim, replace tabs with spaces so we only need to handle
        // space in the following; only if not splitting on TAB
        final String trimmedSource;
        if (splitChar != TAB) {
            trimmedSource = source.replace(TAB, SPACE).trim();
        }
        else {
            trimmedSource = source;
        }

        final String escapedSource = substituteEscapedQuotes(trimmedSource);

        String fullRegex = splitChar + splitRegex;
        if (splitChar == PIPE) {
            fullRegex = "\\" + fullRegex;
        }

        return Pattern.compile(fullRegex)
                .splitAsStream(escapedSource)
                .filter(item -> !item.isEmpty())
                .map(item -> item.trim())
                .map(item -> deleteHeadTailQuotes ? removeQuotes(item) : item)
                .map(item -> revertQuoteSubsitutions(item))
                .toArray(size -> new String[size]);
    }

    /**
     * Remove matching quotes from start/end of input string
     * If there are no quotes or if the leading or trailing
     * quotes do not match, this method has no effect.
     *
     * @param input String to parse
     * @return String with any wrapping quotes removed.
     */
    static String removeQuotes(String input) {

        if (input.startsWith("\"") && input.endsWith("\"") || input.startsWith("'") && input.endsWith("'")) {
            return input.substring(1, input.length() - 1);
        } else {
            return input;
        }
    }

    /**
     * Remove escaped quotes (single and double) from the input string.
     * These may be found inside a 'quoted section' and should not be processed.
     *
     * Replace the character sequences with an unlikely unicode character.
     *
     * @param source String to process
     * @return
     */
    static String substituteEscapedQuotes(String source) {

        return source
                .replaceAll(ESCAPED_QUOTE, SUBSTITUTE_QUOTE)
                .replaceAll(ESCAPED_SINGLE_QUOTE, SUBSTITUTE_SINGLE_QUOTE);

    }

    /**
     * Restore 'escaped quotes' (single and double) removed with
     * {@link StringSplitter.substitueEscapeQuotes}
     *
     * @param source String to process
     * @return
     */
    static String revertQuoteSubsitutions(String input) {
        return input
                .replaceAll(SUBSTITUTE_SINGLE_QUOTE, ESCAPED_SINGLE_QUOTE)
                .replaceAll(SUBSTITUTE_QUOTE, ESCAPED_QUOTE);

    }
}
