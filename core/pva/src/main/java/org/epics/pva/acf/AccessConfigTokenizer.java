/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.acf;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/** Tokenizer for *.acf files
 *  @author Kay Kasemir
 */
class AccessConfigTokenizer implements Closeable
{
    final static List<String> KEYWORDS = List.of("UAG", "HAG", "ASG", "RULE");

    static enum Type { KEYWORD, SEPARATOR, NAME }

    static record Token(Type type, String value)
    {
        /** @return Separator or null-char */
        public char separator()
        {
            if (type == Type.SEPARATOR  &&  value.length() == 1)
                return value.charAt(0);
            return '\0';
        }

        /** @return Keyword or <code>null</code> */
        public String keyword()
        {
            if (type == Type.KEYWORD)
                return value;
            return null;
        }

        @Override
        public final String toString()
        {
            return type + " " + value;
        }
    };

    private final String filename;
    private final BufferedReader reader;
    private String line;
    private int line_no = 0;
    private int column = 0;

    /** @param filename File name (used for error messages)
     *  @param stream_reader {@link Reader}
     *  @throws Exception on error
     */
    public AccessConfigTokenizer(final String filename, final Reader file_reader) throws Exception
    {
        this.filename = filename;
        reader = new BufferedReader(file_reader);
        nextLine();
    }

    /** @return Reached end of file? */
    public boolean done()
    {
        return line == null;
    }

    /** @return Reached end of line? */
    private boolean eol()
    {
        return column >= line.length();
    }

    /** @return Char at current line and column */
    private char current()
    {
        return line.charAt(column);
    }

    /** Read next line */
    private void nextLine() throws Exception
    {
        line = reader.readLine();
        ++line_no;
        column = 0;
        // System.out.println(this + ": " +  line);
    }

    /** @return Next {@link Token} or <code>null</code> at end of file */
    public Token nextToken() throws Exception
    {
        while (! done())
        {
            if (eol())
            {   // Move to next line
                nextLine();
                continue;
            }

            if (Character.isWhitespace(current()))
            {   // Skip spaces
                do
                    ++column;
                while (!eol()  &&  Character.isWhitespace(current()));
                continue;
            }

            if (current() == '#')
            {   // Skip comments
                nextLine();
                continue;
            }

            if ("(){},".indexOf(current()) >= 0)
            {   // Found separator
                final Token token = new Token(Type.SEPARATOR, Character.toString(current()));
                ++column;
                return token;
            }

            if (current() == '"')
            {   // Collect chars of quoted name
                ++column;
                int end = line.indexOf('"', column);
                if (end < 0)
                    throw new Exception(this + " Missing end of quoted string");
                final Token token = new Token(Type.NAME, line.substring(column, end));
                column = end + 1;
                return token;
            }

            // Check for keyword
            final String rest = line.substring(column);
            for (var keyword : KEYWORDS)
                if (rest.startsWith(keyword))
                {
                    final Token token = new Token(Type.KEYWORD, keyword);
                    column += keyword.length();
                    return token;
                }

            // Assume it's a name, which includes "127.0.0.1" or "::1"
            int start = column;
            while (!eol() && (Character.isUpperCase(current()) ||
                              Character.isLowerCase(current()) ||
                              "_0123456789.:".indexOf(current()) >= 0))
                ++column;
            if (start == column)
                throw new Exception(this + " Stuck on invalid character");
            final String name = line.substring(start, column);
            return new Token(Type.NAME, name);
        }
        return null;
    }

    /** @param sep Expected separator */
    public void checkSeparator(final char sep) throws Exception
    {
        final Token token = nextToken();
        if (token == null  ||  token.separator() != sep)
            throw new Exception(this + " Expected '" + sep + "', got " + token);
    }

    /** @return Next name */
    public String nextName() throws Exception
    {
        final Token token = nextToken();
        if (token == null  ||  token.type() != Type.NAME  ||  token.value().isBlank())
            throw new Exception(this + " Expected name, got " + token);
        return token.value();
    }

    @Override
    public void close() throws IOException
    {
        reader.close();
    }

    @Override
    public String toString()
    {
        return filename + " " + line_no + "," + column;
    }
}
