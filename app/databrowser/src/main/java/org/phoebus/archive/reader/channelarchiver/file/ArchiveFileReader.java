/*******************************************************************************
 * Copyright (c) 2017-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver.file;

import static org.phoebus.archive.reader.ArchiveReaders.logger;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;

/** ArchiveReader for Channel Archiver index & data files.
 *  @author Kay Kasemir
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ArchiveFileReader implements ArchiveReader
{
    private final ArchiveFileIndexReader indexReader;

    /** Construct an ArchiveFileReader.
     *  @param index Path to  Channel Archiver index file
     *  @throws IOException
     */
    public ArchiveFileReader(final File index) throws IOException
    {
        indexReader = new ArchiveFileIndexReader(index);
    }

    @Override
    public String getDescription()
    {
        return "Channel Archiver";
    }

    @Override
    public List<String> getNamesByPattern(final String glob_pattern) throws Exception
    {
        final String reg_exp = glob_pattern.replace("\\", "\\\\")
                                           .replace(".", "\\.")
                                           .replace("*", ".*")
                                           .replace("?", ".");
        final Pattern pattern = Pattern.compile(reg_exp, Pattern.CASE_INSENSITIVE);
        final List<String> result = new ArrayList<>();
        for (String name : indexReader.getChannelNames())
            if (pattern.matcher(name).matches())
                result.add(name);
        return result;
    }

    @Override
    public ValueIterator getRawValues(final String name, final Instant start, final Instant end)
            throws UnknownChannelException, Exception
    {
        final List<DataFileEntry> entries = indexReader.getEntries(name, start, end);
        // entries.forEach(System.out::println);
        return new ArchiveFileSampleReader(start, end, entries);
    }

    @Override
    public void close()
    {
        try
        {
            indexReader.close();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot close index", ex);
        }
    }
}
