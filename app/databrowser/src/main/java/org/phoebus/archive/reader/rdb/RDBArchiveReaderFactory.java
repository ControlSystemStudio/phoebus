/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.rdb;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;

/** SPI for "jdbc:" archive URLs
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RDBArchiveReaderFactory implements ArchiveReaderFactory
{
    @Override
    public String getPrefix()
    {
        return "jdbc:";
    }

    @Override
    public ArchiveReader createReader(final String url) throws Exception
    {
        return new RDBArchiveReader(url);
    }
}
