/*******************************************************************************
 * Copyright (c) 2017-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver.file;

import java.io.File;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;

/** SPI for "cadf:" archive URLs
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArchiveFileReaderFactory implements ArchiveReaderFactory
{
    public final static String PREFIX = "cadf:";

    @Override
    public String getPrefix()
    {
        return PREFIX;
    }

    @Override
    public ArchiveReader createReader(final String url) throws Exception
    {
        final File index = new File(url.substring(PREFIX.length()));
        return createReader(index);
    }

    static ArchiveReader createReader(final File index) throws Exception
    {
        if (index.getName().endsWith(".xml"))
            return new ListIndexReader(index);
        else
            return new ArchiveFileReader(index);
    }
}
