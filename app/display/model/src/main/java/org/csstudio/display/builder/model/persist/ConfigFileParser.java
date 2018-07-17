/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/** Helper for parsing basic "name = ..." configuration files
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class ConfigFileParser
{
    /** Read from stream
     *  @param stream Stream to read
     *  @throws Exception on error
     */
    protected void read(final InputStream stream) throws Exception
    {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ( (line = reader.readLine())  != null)
        {
            line = line.trim();
            if (isComment(line))
                continue;

            final int sep = line.indexOf('=');
            if (sep < 0)
                continue;

            final String name = line.substring(0, sep).trim();
            final String value = line.substring(sep + 1).trim();
            parse(name, value);
        }
    }

    /** Parse a "name = value" element
     *  @param name Name
     *  @param value Value
     *  @throws Exception on error
     */
    abstract protected void parse(String name, String value) throws Exception;

    /** @param line One line in the configuration
     *  @return <code>true</code> if line looks like a comment
     */
    private static boolean isComment(final String line)
    {
        return line.startsWith("#")  ||  line.startsWith("//");
    }
}
