/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.data;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** {@link ReadWriteLock} with debug information
 *
 *  <p>Suggested use:
 *  Access the read or write lock with <code>tryLock(..)</code>
 *  and print the lock on failure to capture the state
 *  (owner, pending threads) in the log.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class InstrumentedReadWriteLock extends ReentrantReadWriteLock
{
    private static final long serialVersionUID = 1L;

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("Read/Write lock, ");
        final Thread owner = getOwner();
        if (owner == null)
            buf.append("no owner");
        else
            buf.append("owned by ").append(owner);
        Collection<Thread> threads = getQueuedReaderThreads();
        if (threads.isEmpty())
            buf.append(", no pending readers");
        else
        {
            buf.append(", pending readers (");
            boolean first = true;
            for (Thread thread : threads)
            {
                if (! first)
                    buf.append(", ");
                buf.append(thread);
                first = false;
            }
            buf.append(")");
        }

        threads = getQueuedWriterThreads();
        if (threads.isEmpty())
            buf.append(", no pending writers");
        else
        {
            buf.append(", pending writers (");
            boolean first = true;
            for (Thread thread : threads)
            {
                if (! first)
                    buf.append(", ");
                buf.append(thread);
                first = false;
            }
            buf.append(")");
        }

        return buf.toString();
    }
}
