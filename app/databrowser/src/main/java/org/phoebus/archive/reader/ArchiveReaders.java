/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.archive.reader.spi.ArchiveReaderFactory;

/** Access to {@link ArchiveReader}s via SPI
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArchiveReaders
{
    /** Suggested logger for all archive reader code */
    public static final Logger logger = Logger.getLogger(ArchiveReaders.class.getPackageName());

    private static final List<ArchiveReaderFactory> factories = new ArrayList<>();

    static
    {
        for (ArchiveReaderFactory factory : ServiceLoader.load(ArchiveReaderFactory.class))
        {
            logger.log(Level.CONFIG, "ArchiveReader for '" + factory.getPrefix() + "'");
            factories.add(factory);
        }
    }

    private ArchiveReaders()
    {
    }

    public static ArchiveReader createReader(final String url) throws Exception
    {
        for (ArchiveReaderFactory factory : factories)
            if (url.startsWith(factory.getPrefix()))
                return factory.createReader(url);
        throw new Exception("No archive reader for '" + url + "'");
    }
}
