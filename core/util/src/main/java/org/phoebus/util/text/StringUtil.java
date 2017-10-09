/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A set of utilities to parse strings.
 *
 * @author carcassi
 */
public class StringUtil {

    private StringUtil() {
        // Prevent instantiation
    }

    /**
     * The pattern of a string fragment with escape sequences.
     */
    public static final String STRING_ESCAPE_SEQUENCE_REGEX = "\\\\(\"|\\\\|\'|r|n|b|t|u[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]|[0-3]?[0-7]?[0-7])";

    /**
     * The pattern of a string, including double quotes.
     */
    public static final String QUOTED_STRING_REGEX = "\"([^\"\\\\]|" + StringUtil.STRING_ESCAPE_SEQUENCE_REGEX + ")*\"";

    /**
     * The pattern of a string using single quotes.
     */
    public static final String SINGLEQUOTED_STRING_REGEX = "\'([^\"\\\\]|" + StringUtil.STRING_ESCAPE_SEQUENCE_REGEX + ")*\'";

    /**
     * The pattern of a double value.
     */
    public static final String DOUBLE_REGEX = "([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)";

    /**
     * The pattern of a double value.
     */
    public static final String DOUBLE_REGEX_WITH_NAN = "([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)|NaN";

    static Pattern escapeSequence = Pattern.compile(STRING_ESCAPE_SEQUENCE_REGEX);

    /**
     * Takes a single quoted or double quoted String and returns the unquoted
     * and unescaped version of the string.
     *
     * @param quotedString the original string
     * @return the unquoted string
     */
    public static String unquote(String quotedString) {
        return unescapeString(quotedString.substring(1, quotedString.length() - 1));
    }

    /**
     * Takes an escaped string and returns the unescaped version
     *
     * @param escapedString the original string
     * @return the unescaped string
     */
    public static String unescapeString(String escapedString) {
        Matcher match = escapeSequence.matcher(escapedString);
        StringBuffer output = new StringBuffer();
        while(match.find()) {
            match.appendReplacement(output, substitution(match.group()));
        }
        match.appendTail(output);
        return output.toString();
    }

    private static String substitution(String escapedToken) {
        switch (escapedToken) {
            case "\\\"":
                return "\"";
            case "\\\\":
                return "\\\\";
            case "\\\'":
                return "\'";
            case "\\r":
                return "\r";
            case "\\n":
                return "\n";
            case "\\b":
                return "\b";
            case "\\t":
                return "\t";
        }
        if (escapedToken.startsWith("\\u")) {
            // It seems that you can't use replace with an escaped
            // unicode sequence. Bug in Java?
            // Parsing myself
            return Character.toString((char) Long.parseLong(escapedToken.substring(2), 16));
        }
        return Character.toString((char) Long.parseLong(escapedToken.substring(1), 8));
    }

    /**
     * Parses a line of text representing comma separated values and returns
     * the values themselves.
     *
     * @param line the line to parse
     * @param separatorRegex the regular expression for the separator
     * @return the list of values
     */
    public static List<Object> parseCSVLine(String line, String separatorRegex) {
        List<Object> matches = new ArrayList<>();
        int currentPosition = 0;
        Matcher separatorMatcher = Pattern.compile("^" + separatorRegex).matcher(line);
        Matcher stringMatcher = Pattern.compile("^" + QUOTED_STRING_REGEX).matcher(line);
        Matcher doubleMatcher = Pattern.compile("^" + DOUBLE_REGEX).matcher(line);
        while (currentPosition < line.length()) {
            if (stringMatcher.region(currentPosition, line.length()).useAnchoringBounds(true).find()) {
                // Found String match
                String token = line.substring(currentPosition + 1, stringMatcher.end() - 1);
                matches.add(unescapeString(token));
                currentPosition = stringMatcher.end();
            } else if (doubleMatcher.region(currentPosition, line.length()).useAnchoringBounds(true).find()) {
                // Found Double match
                Double token = Double.parseDouble(line.substring(currentPosition, doubleMatcher.end()));
                matches.add(token);
                currentPosition = doubleMatcher.end();
            } else {
                throw new IllegalArgumentException("Can't parse line: expected token at " + currentPosition + " (" + line + ")");
            }

            if (currentPosition < line.length()) {
                if (!separatorMatcher.region(currentPosition, line.length()).useAnchoringBounds(true).find()) {
                    throw new IllegalArgumentException("Can't parse line: expected separator at " + currentPosition + " (" + line + ")");
                }
                currentPosition = separatorMatcher.end();
            }
        }
        return matches;
    }

}
