/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver.file;

import java.io.File;

/** Information about one data file entry
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
class DataFileEntry
{
    /** Data file */
    final File file;

    /** Offset of DataHeader */
    final long offset;

    public DataFileEntry(final File file, final long offset)
    {
        this.file = file;
        this.offset = offset;
    }

    @Override
    public String toString()
    {
        return String.format("DataFileEntry in '%s' @ 0x%08x (%d)", file.getName(), offset, offset);
    }
}