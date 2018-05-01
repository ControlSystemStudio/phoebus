/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula;

import java.io.StringReader;

/** String scanner for formula usage.
 *  <p>
 *  Returns one character at a time,
 *  skips spaces and linefeeds,
 *  and allows to 'get' the current char
 *  without advancing.
 *
 *  @author Kay Kasemir
 */
class Scanner
{
    private static final String to_skip = " \t\n"; //$NON-NLS-1$
    private StringReader reader;
    private int current;
    private boolean done;

    /** Create, initialize with string, position on first character. */
    public Scanner(String s) throws Exception
    {
        reader = new StringReader(s);
        done = false;
        next();
    }

    /** @return Returns the current character. */
    public char get()
    {
        return (char)current;
    }

    /** Move to the next character (skipping spaces). */
    public void next() throws Exception
    {
        try
        {
            do
                current = reader.read();
            while (to_skip.indexOf(current) >= 0);
            if (current == -1)
                done = true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            done = true;
            throw new Exception("Internal parser error: " + ex.getMessage()); //$NON-NLS-1$
        }
    }

    /** @return Returns <code>true</code> when reaching the end of the string. */
    public boolean isDone()
    {
        return done;
    }

    /** @return Returns the remaining string from the current char on. */
    public String rest() throws Exception
    {
        StringBuffer buf = new StringBuffer();
        while (!isDone())
        {
            buf.append(get());
            next();
        }
        return buf.toString();
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        if (done)
            return "Scanner is done";
        return String.format("Scanner on '%c'", current);
    }
}
