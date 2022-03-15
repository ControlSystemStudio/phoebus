/*******************************************************************************
 * Copyright (c) 2021-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * are made available under the terms of the Eclipse Public License v1.0
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.ts.reader;

import java.util.logging.Logger;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;

/** Archive reader factory for TimescaleDB
 *
 *  <p>TimescaleDB uses the same type of URL as
 *  other Postgres connections,
 *  <code>jdbc:postgresql://my.host.org:5432/tsarch</code>.
 *  The prefix <code>ts:</code> is added to select
 *  the timescale-enhanced reader, and the remaining URL
 *  is then used as-is for the actual database connection.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TSArchiveReaderFactory implements ArchiveReaderFactory
{
    /** Common logger */
    public static final Logger logger = Logger.getLogger(TSArchiveReaderFactory.class.getPackageName());

    public static final String PREFIX = "ts:";

    @Override
    public String getPrefix()
    {
        return PREFIX;
    }

    @Override
    public ArchiveReader createReader(final String url) throws Exception
    {
        return new TSArchiveReader(url);
    }
}
