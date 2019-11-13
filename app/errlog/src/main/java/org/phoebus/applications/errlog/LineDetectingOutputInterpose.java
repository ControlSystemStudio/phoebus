/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.errlog;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Consumer;

/** Output stream interpose
 *
 *  <p>Copies written data to an original output stream.
 *  Calls handler for each complete line.
 *
 *  <p>Closing this stream will NOT close the original stream.
 *
 *  @author Kay Kasemir
 */
public class LineDetectingOutputInterpose extends OutputStream
{
    final private PrintStream original;
    final private Consumer<String> line_handler;
    private byte[] linebuf = new byte[512];
    private int count = 0;

    /** @param original Original stream to which data is forwarded
     *  @param line_handler Called for each complete line of text
     */
    public LineDetectingOutputInterpose(final PrintStream original,
                                        final Consumer<String> line_handler)
    {
        this.original = original;
        this.line_handler = line_handler;
    }

    private void ensureCapacity(final int size)
    {
        if (size > linebuf.length)
        {
            // Grow by doubling current size
            int next = linebuf.length * 2;
            if (size > next)
                next = size;
            linebuf = Arrays.copyOf(linebuf, next);
        }
    }

    @Override
    public synchronized void write(final int b) throws IOException
    {
        original.write(b);

        // Reached end of a line?
        if (b == '\n')
        {
            line_handler.accept(new String(linebuf, 0, count));
            count = 0;
        }
        else
        {
            ensureCapacity(count + 1);
            linebuf[count] = (byte) b;
            count += 1;
        }
    }
}
