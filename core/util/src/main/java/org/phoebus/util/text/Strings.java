package org.phoebus.util.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Strings {

    /**
     * Based on the the google String.isNullOrEmpty.
     *
     * @param str
     * @return true if the string is null or empty
     */
    public static boolean isNullOrEmpty(String str) {
        if(Objects.isNull(str) || str.isBlank()) {
            return true;
        } else {
            return false;
        }
    }

    public static List<String> parseStringList(String text) {
        final List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }

        final StringBuilder current = new StringBuilder();
        Character quote = null;

        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);

            if (quote == null) {
                if (Character.isWhitespace(c) && current.isEmpty()) {
                    // skip non-quoted whitespace character at the start of unquoted string
                }
                else if (c == '\"' || c == '\'') {
                    // start quoted string, assert that current StringBuilder is empty
                    if (!current.isEmpty()) {
                        // string contained non-whitespace, characters so it was of the form
                        // "input 'start quote', next element"
                        // which is invalid
                        throw new RuntimeException("Unexpected quote in non-quoted string: " + current + c);
                    }
                    quote = c;
                }
                else if (c == ',') {
                    // end unquoted string, add trimmed value and reset
                    result.add(current.toString().trim());
                    current.setLength(0);
                }
                else {
                    // other character, just add to the current value
                    current.append(c);
                }
            }
            else {
                if (c == quote) {
                    // end quoted string, expect whitespace then comma
                    String elt = current.toString();
                    result.add(elt);
                    current.setLength(0);

                    // also reset quote
                    quote = null;

                    while (true) {
                        i++;
                        if (i >= text.length()) {
                            // end of string, no more elements after this
                            return result;
                        }
                        final char k = text.charAt(i);
                        if (k == ',') {
                            // start next element
                            break;
                        }
                        if (Character.isWhitespace(k)) {
                            // skip whitespace character in search for comma
                            continue;
                        }
                        // non-whitespace, non-comma character, BAD!
                        throw new RuntimeException(
                                "Expected whitespace after string list element " + elt + " got " + k
                        );
                    }
                }
                else if (c == '\\') {
                    // escaped quote?
                    // we might want to support more escape sequences
                    if (i + 1 < text.length() && text.charAt(i + i) == quote) {
                        current.append(quote);
                        i++;
                    }
                    else {
                        // just a backslash...
                        current.append(c);
                    }
                }
                else {
                    // other character, don't do anything special
                    current.append(c);
                }
            }
        }

        // last element may still be in here
        if (!current.isEmpty()) {
            if (quote != null) {
                throw new RuntimeException(
                        "Unexpected end of string while scanning quoted string " + quote + current
                );
            }
            else {
                // non-quoted, just add trimmed value
                result.add(current.toString().trim());
            }
        }

        return result;
    }
}
