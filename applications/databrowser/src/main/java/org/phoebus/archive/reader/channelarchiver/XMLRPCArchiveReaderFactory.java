/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;

/** SPI for "xnds:" archive URLs
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLRPCArchiveReaderFactory implements ArchiveReaderFactory
{
    public static final String PREFIX = "xnds:";

    @Override
    public String getPrefix()
    {
        return PREFIX;
    }

    @Override
    public ArchiveReader createReader(final String url) throws Exception
    {
        return new XMLRPCArchiveReader(url);
    }
}
