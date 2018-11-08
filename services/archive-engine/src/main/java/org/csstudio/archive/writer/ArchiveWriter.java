/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer;

import org.epics.vtype.VType;

/** Interface for writing samples to an archive
 *
 *  @author Kay Kasemir
 */
public interface ArchiveWriter
{
    /** Obtain a channel to which samples can be written
     *  @param name Channel name (typically a PV name)
     *  @return {@link WriteChannel}, not <code>null</code>
     *  @throws Exception on error, for example failure to access
     *          the data store, or a new, previously unknown channel name
     *          could not be added to the archive
     */
    public WriteChannel getChannel(String name) throws Exception;

    public default WriteChannel getChannel(String name, String retention) throws Exception
    {
        return getChannel(name);
    }

    /** Add a sample to the archive.
     *
     *  <p>The underlying implementation might optimize
     *  and not actually write anything until <code>flush()</code>
     *  is called.
     *  @param channel Channel to which to add a sample
     *  @param sample Value to add
     *  @throws Exception on error, for example failure to access
     *          the data store, or the sample could not be added
     *          to the archive
     */
    public void addSample(WriteChannel channel, VType sample) throws Exception;

    /** Write all recently added samples to the archive.
     *
     *  <p>Since the underlying implementation is allowed to
     *  optimize, it might buffer the added samples, or
     *  queue them by data type, and an explicit call
     *  to this method is required to flush them out
     *  to the archive storage.
     *  @throws Exception on error, for example failure to access
     *          the data store. Samples added since the last
     *          <code>flush()</code> are likely lost
     */
    public void flush() throws Exception;

    /** Should be called to release resources,
     *  for example disconnect from a relational database.
     */
    public void close();
}
