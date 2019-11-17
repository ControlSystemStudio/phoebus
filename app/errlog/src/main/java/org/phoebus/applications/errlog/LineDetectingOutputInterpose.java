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
    // Tried to implement using PipedInputStream & PipedOutputStream,
    // but those are expected to be called by only one thread each.
    // Implementations fetches `Thread.currentThread()` when written,
    // and reader then checks at certain times if that thread is still alive.
    // When the redirected System.out is thus called from a short-term thread,
    // the pipe will report "Write end dead" or "Pipe broken" once that thread exits.
    // https://stackoverflow.com/questions/1866255/pipedinputstream-how-to-avoid-java-io-ioexception-pipe-broken/1867063#1867063
    // https://bugs.openjdk.java.net/browse/JDK-4028322

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
        else if (b != '\r')
        {
            // Skip Windows line feed, but add other text to line
            ensureCapacity(count + 1);
            linebuf[count] = (byte) b;
            count += 1;
        }
    }
}
