/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver;

import java.net.URL;
import java.time.Instant;
import java.util.List;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;

public class XMLRPCArchiveReader implements ArchiveReader
{
    private final URL url;

    public XMLRPCArchiveReader(final String url) throws Exception
    {
        this.url = new URL("http" + url.substring(4));
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getNamesByPattern(String glob_pattern) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ValueIterator getRawValues(String name, Instant start, Instant end)
            throws UnknownChannelException, Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void cancel()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void close()
    {
        // TODO Auto-generated method stub

    }

}
