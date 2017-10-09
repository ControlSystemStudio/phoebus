/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.phoebus.util.array.ArrayDouble;
import org.phoebus.util.array.ListDouble;
import static org.phoebus.util.text.StringUtil.DOUBLE_REGEX_WITH_NAN;

/**
 * Utility class to parse CSV text. The parser is thread safe: it includes an
 * immutable set of parameters and the state for each parsing is kept separate.
 * A change in the parser parameters will create a new parser, so to create
 * your configuration take the closest matching as a template and apply the
 * difference.
 * <p>
 * Since there is no CSV strict format, this parser honors as best it
 * can the suggestions found in <a href="http://tools.ietf.org/html/rfc4180">RFC4180</a>,
 * in the <a href="http://en.wikipedia.org/wiki/Comma-separated_values">CSV wikipedia article</a>
 * and other sources.
 * <p>
 * The parser can try multiple separators, so that it can auto-detect the
 * likely correct one. It does so by trying them one by one, checking
 * that it finds more than one column and that all the rows have the same
 * number of columns. If not, proceeds to the next separator.
 * <p>
 * Typical use of the parser:
 * <blockquote><pre>
 * CsvParserResult result = CsvParser.AUTOMATIC
 *   .withHeader(CsvParser.Header.NONE)
 *   .parse(new FileReader("table.csv"));</pre></blockquote>
 * <p>
 * The parsing of each line is based on code and insights found in
 * <a href="http://regex.info/book.html"> Mastering Regular Expressions</a>.
 *
 * @author carcassi
 */
public class CsvParser {

    // Configuration
    private final String separators;
    private final Header header;

    /**
     * The configuration options for the header.
     */
    public enum Header {
        /**
         * Auto detects whether the first line is a header.
         * <p>
         * The first line is interpreted as data only if it can be safely
         * distinguished. If all columns contain strings, then the first
         * line is always interpreted as a header. If the types in the
         * first line do not match the column (e.g. first line string, rest are
         * numbers) then it is interpreted as header. If the types match,
         * and one of them is not a string (e.g. number) then the first
         * line is interpreted as data.
         */
        AUTO,

        /**
         * The first line is the header.
         */
        FIRST_LINE,

        /**
         * The data contains no header, and the first line is data.
         * <p>
         * A header is automatically generated with the convention given by
         * spreadsheets columns: A, B, ..., Y, Z, AA, AB, ..., AZ, BA, and so on.
         */
        NONE};

    private class State {
        // Parser state
        private int nColumns;
        private boolean columnMismatch = false;
        private List<String> columnNames;
        private List<Boolean> columnNumberParsable;
        private List<Boolean> columnTimestampParsable;
        private List<List<String>> columnTokens;
        private String currentSeparator;

        // Regex object used for parsing
        private Matcher mLineTokens;
        private final Matcher mQuote = pQuote.matcher("");
        private final Matcher mDouble = pDouble.matcher("");

        // Keep data on best matched separator
        private String bestSeparator;
        private int bestNLines = -1;
    }


    private static final Pattern pQuote = Pattern.compile("\"\"");
    private static final Pattern pDouble = Pattern.compile(DOUBLE_REGEX_WITH_NAN);

    /**
     * Automatic parser: auto-detects whether the first line is a header or not
     * and tries the most common separators (i.e. ',' ';' 'TAB' 'SPACE').
     */
    public static final CsvParser AUTOMATIC = new CsvParser(",;\t ", Header.AUTO);

    private CsvParser(String separators, Header header) {
        this.separators = separators;
        this.header = header;
    }

    /**
     * Returns the list of separators that are going to be tried while parsing.
     *
     * @return a string with all the possible separators
     */
    public String getSeparators() {
        return separators;
    }

    /**
     * Creates a new parser that uses the given separators.
     * <p>
     * Each character of the string is tried until the parsing is
     * successful.
     *
     * @param separators the new list of separators
     * @return a new parser
     */
    public CsvParser withSeparators(String separators) {
        return new CsvParser(separators, header);
    }

    /**
     * Returns the way that the parser handles the header (the first line of
     * the csv file).
     *
     * @return the header configuration of the parser
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Creates a new parser with the given header handling.
     *
     * @param header the header configuration for the parser
     * @return a new parser
     */
    public CsvParser withHeader(Header header) {
        return new CsvParser(separators, header);
    }


    /**
     * Parser the text provided by the reader with the format defined in this
     * parser. This method is thread-safe.
     * <p>
     * If the parsing fails, this method does not throw an exception but
     * will have information in the result. The idea is that, in the future,
     * the parser can provide multiple reasons as why the parsing failed or
     * event incomplete results.
     *
     * @param reader a reader
     * @return the parsed information
     */
    public CsvParserResult parse(Reader reader) {
        // State used for parsing. Since each call has its own state,
        // the parsing is thread safe.
        State state = new State();

        // Divide into lines.
        // Note that means we are going to keep in memory the whole file.
        // This is not very memory efficient. But since we have to do multiple
        // passes to find the right separator, we don't have much choice.
        // Also: the actual parsed result will need to stay in memory anyway.
        List<String> lines = csvLines(reader);

        // Try each seaparater
        separatorLoop:
        for(int nSeparator = 0; nSeparator < getSeparators().length(); nSeparator++) {
            state.currentSeparator = getSeparators().substring(nSeparator, nSeparator+1);

            // Taken from Mastering Regular Exceptions
            // Disabled comments so that space could work as possible separator
            String regex = // puts a doublequoted field in group(1) and an unquoted field into group(2)
                    // Start with beginning of line or separator
                    "\\G(?:^|" + state.currentSeparator + ")" +
                    // Match a quoted string
                    "(?:" +
                    "\"" +
                    "((?:[^\"]++|\"\")*+)" +
                    "\"" +
                    // Or match a string without the separator
                    "|" +
                    "([^\"" + state.currentSeparator + "]*)" +
                    ")";
            // Compile the matcher once for all the parsing
            state.mLineTokens = Pattern.compile(regex).matcher("");

            // Try to parse the first line (the titles)
            // If only one columns is found, proceed to next separator
            state.columnNames = parseTitles(state, lines.get(0));
            state.nColumns = state.columnNames.size();
            if (state.nColumns == 1) {
                continue;
            }

            // Prepare the data structures to hold column data while parsing
            state.columnMismatch = false;
            state.columnNumberParsable = new ArrayList<>(state.nColumns);
            state.columnTimestampParsable = new ArrayList<>(state.nColumns);
            state.columnTokens = new ArrayList<>();
            for (int i = 0; i < state.nColumns; i++) {
                state.columnNumberParsable.add(true);
                state.columnTimestampParsable.add(false);
                state.columnTokens.add(new ArrayList<String>());
            }

            // Parse each line
            // If one line does not match the number of columns found in the first
            // line, pass to the next separator
            for (int i = 1; i < lines.size(); i++) {
                parseLine(state, lines.get(i));
                if (state.columnMismatch) {
                    if (i > state.bestNLines) {
                        state.bestSeparator =  state.currentSeparator;
                        state.bestNLines = i;
                    }
                    continue separatorLoop;
                }
            }

            // The parsing succeeded! No need to try other separator
            break;

        }

        // We are out of the loop: did we end because we parsed correctly,
        // or because even the last separator was a mismatch?
        if (state.columnMismatch) {
            return new CsvParserResult(null, null, null, 0, false, "Parsing failed: number of columns not constant. Using separator '"
                    + state.bestSeparator + "', line " + (state.bestNLines + 1));
        }

        // Parsing was successful.
        // Should the first line be used as data?
        if (header == Header.NONE || (header == Header.AUTO && isFirstLineData(state, state.columnNames))) {
            for (int i = 0; i < state.nColumns; i++) {
                state.columnTokens.set(i, joinList(state.columnNames.get(i), state.columnTokens.get(i)));
                state.columnNames.set(i, alphabeticName(i));
            }
        }

        // Now it's time to convert the tokens to the actual type.
        List<Object> columnValues = new ArrayList<>(state.nColumns);
        List<Class<?>> columnTypes = new ArrayList<>(state.nColumns);
        for (int i = 0; i < state.nColumns; i++) {
            if (state.columnNumberParsable.get(i)) {
                columnValues.add(convertToListDouble(state.columnTokens.get(i)));
                columnTypes.add(double.class);
            } else {
                columnValues.add(state.columnTokens.get(i));
                columnTypes.add(String.class);
            }
        }

        // Prepare result, and remember to clear the state, so
        // we don't keep references to junk
        CsvParserResult result = new CsvParserResult(state.columnNames, columnValues, columnTypes, state.columnTokens.get(0).size(), true, null);
        return result;
    }

    /**
     * Given a list of tokens, convert them to a list of numbers.
     *
     * @param tokens the tokens to be converted
     * @return the number list
     */
    private ListDouble convertToListDouble(List<String> tokens) {
        double[] values = new double[tokens.size()];
        for (int i = 0; i < values.length; i++) {
            if (tokens.get(i).isEmpty()) {
                values[i] = Double.NaN;
            } else {
                values[i] = Double.parseDouble(tokens.get(i));
            }
        }
        return new ArrayDouble(values);
    }

    /**
     * Divides the whole text into lines.
     *
     * @param reader the source of text
     * @return the lines
     */
    static List<String> csvLines(Reader reader) {
        // This needs to handle quoted text that spans multiple lines,
        // so we divide the full text into chunks that correspond to
        // a single csv line
        try {
            BufferedReader br = new BufferedReader(reader);
            List<String> lines = new ArrayList<>();
            // The current line read from the Reader
            String line;
            // The full csv line that may span multiple lines
            String longLine = null;
            while ((line = br.readLine()) != null) {
                // If we have a line from the previous iteration,
                // we concatenate it
                if (longLine == null) {
                    longLine = line;
                } else {
                    longLine = longLine.concat("\n").concat(line);
                }
                // Count the number of quotes: if it's even, the csv line
                // must end here. If not, it will continue to the next
                if (isEvenQuotes(longLine)) {
                    lines.add(longLine);
                    longLine = null;
                }
            }
            // If there is text leftover, the line was not closed propertly.
            // XXX: we need to figure out how to handle errors like this
            if (longLine != null) {
                lines.add(longLine);
            }
            return lines;
        } catch(IOException ex) {
            throw new RuntimeException("Couldn't process data", ex);
        }
    }

    /**
     * Determines whether the string contains an even number of double quote
     * characters.
     *
     * @param string the given string
     * @return true if contains even number of '"'
     */
    static boolean isEvenQuotes(String string) {
        // In principle, we could use the regex given by:
        // Pattern pEvenQuotes = Pattern.compile("([^\"]*\\\"[^\"]*\\\")*[^\"]*");
        // We assume just counting the instances of double quotes is more efficient
        // but we haven't really tested that assumption.

        boolean even = true;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == '\"') {
                even = !even;
            }
        }
        return even;
    }

    /**
     * Parses the first line to get the column names.
     *
     * @param line the text line
     * @return the column names
     */
    private List<String> parseTitles(State state, String line) {
        // Match using the parser
        List<String> titles = new ArrayList<>();
        state.mLineTokens.reset(line);
        while (state.mLineTokens.find()) {
            String value;
            if (state.mLineTokens.start(2) >= 0) {
                value = state.mLineTokens.group(2);
            } else {
                // If quoted, always use string
                value = state.mQuote.reset(state.mLineTokens.group(1)).replaceAll("\"");
            }
            titles.add(value);
        }
        return titles;
    }

    /**
     * Parses a line, saving the tokens, and determines the type match.
     *
     * @param line a new line
     */
    private void parseLine(State state, String line) {
        // XXX The regex does not work if the first token is blank, and I
        // don't understand why. Workaround: if it's blank, add a space,
        // and remember I added a space.
        boolean firstEmpty = false;
        if (line.startsWith(state.currentSeparator)) {
            line = " " + line;
            firstEmpty = true;
        }

        // Match using the parser
        state.mLineTokens.reset(line);
        int nColumn = 0;
        while (state.mLineTokens.find()) {
            // Does this line have more columns than expected?
            if (nColumn == state.nColumns) {
                state.columnMismatch = true;
                return;
            }

            String token;
            if (state.mLineTokens.start(2) >= 0) {
                // The token was unquoted. Check if it could be a number.
                token = state.mLineTokens.group(2);
                if (firstEmpty) {
                    token = "";
                    firstEmpty = false;
                }
                if (!isTokenNumberParsable(state, token)) {
                    state.columnNumberParsable.set(nColumn, false);
                }
            } else {
                // If quoted, always use string
                token = state.mQuote.reset(state.mLineTokens.group(1)).replaceAll("\"");
                state.columnNumberParsable.set(nColumn, false);
            }
            state.columnTokens.get(nColumn).add(token);
            nColumn++;
        }
        // Does this line have fewer columns than expected?
        if (nColumn != state.nColumns) {
            state.columnMismatch = true;
        }
    }

    /**
     * Check whether the token can be parsed to a number.
     *
     * @param state the state of the parser
     * @param token the token
     * @return true if token matches a double
     */
    private boolean isTokenNumberParsable(State state, String token) {
        if (token.isEmpty()) {
            return true;
        }
        return state.mDouble.reset(token).matches();
    }

    /**
     * Checks whether the header can be safely interpreted as data.
     * This is used for the auto header detection.
     *
     * @param state the state of the parser
     * @param headerTokens the header
     * @return true if header should be handled as data
     */
    private boolean isFirstLineData(State state, List<String> headerTokens) {
        // Check whether the type of the header match the type of the following data
        boolean headerCompatible = true;
        // Check whether if all types where strings
        boolean allStrings = true;
        for (int i = 0; i < state.nColumns; i++) {
            if (state.columnNumberParsable.get(i)) {
                allStrings = false;
                if (!isTokenNumberParsable(state, headerTokens.get(i))) {
                    headerCompatible = false;
                }
            }
        }
        // If all columns are strings, it's impossible to tell whether we have
        // a header or not: assume we have a header.
        // If the column types matches (e.g. the header for a number column is also
        // a number) then we'll assume the header is actually data.
        return !allStrings && headerCompatible;
    }

    /**
     * Takes an elements and a list and returns a new list with both.
     *
     * @param head the first element
     * @param tail the rest of the elements
     * @return a list with all elements
     */
    private List<String> joinList(final String head, final List<String> tail) {
        return new AbstractList<String>() {

            @Override
            public String get(int index) {
                if (index == 0) {
                    return head;
                } else {
                    return tail.get(index - 1);
                }
            }

            @Override
            public int size() {
                return tail.size()+1;
            }
        };
    }

    static String alphabeticName(int i) {
        String name = "";
        while (true) {
            int offset = i % 26;
            i = i / 26;
            char character = (char) ('A' + offset);
            name = name + character;
            if (i == 0) {
                return name;
            }
        }
    }

    /**
     * Parses a line of text representing comma separated values and returns
     * the values themselves.
     *
     * @param line the line to parse
     * @param separatorChar the regular expression for the separator
     * @return the list of values
     */
    public static List<Object> parseCSVLine(String line, String separatorChar) {
        String regex = // puts a doublequoted field in group(1) and an unquoted field into group(2)
                "\\G(?:^|" + separatorChar + ")" +
                "(?:" +
                "\"" +
                "((?:[^\"]++|\"\")*+)" +
                "\"" +
                "|" +
                "([^\"" + separatorChar + "]*)" +
                ")";
        Matcher mMain = Pattern.compile(regex).matcher("");
        Matcher mQuote = Pattern.compile("\"\"").matcher("");
        Matcher mDouble = Pattern.compile(DOUBLE_REGEX_WITH_NAN).matcher("");

        List<Object> values = new ArrayList<>();
        mMain.reset(line);
        while (mMain.find()) {
            Object value;
            if (mMain.start(2) >= 0) {
                String field = mMain.group(2);
                if (mDouble.reset(field).matches()) {
                    value = Double.parseDouble(field);
                } else {
                    value = field;
                }
            } else {
                // If quoted, always use string
                value = mQuote.reset(mMain.group(1)).replaceAll("\"");
            }
            values.add(value);
        }
        return values;
    }
}
