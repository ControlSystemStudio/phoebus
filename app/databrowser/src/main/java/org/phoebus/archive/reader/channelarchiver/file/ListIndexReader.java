/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver.file;

import static org.phoebus.archive.reader.ArchiveReaders.logger;

import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.MergingValueIterator;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** ArchiveReader for Channel Archiver index lists
 *
 *  <p>Reads an XML 'indexconfig' that lists binary indices:
 *  <pre>
 *  &lt;indexconfig&gt;
 *    &lt;archive&gt;
 *      &lt;index&gt;relative/path/to/index&lt;/index&gt;
 *    &lt;/archive&gt;
 *  &lt;/indexconfig&gt;
 *  </pre>
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ListIndexReader implements ArchiveReader
{
    private final List<ArchiveReader> archives = new ArrayList<>();

    public ListIndexReader(final File indexlist) throws Exception
    {
        final Element xml = XMLUtil.openXMLDocument(new FileInputStream(indexlist), "indexconfig");

        for (Element arch : XMLUtil.getChildElements(xml, "archive"))
        {
            final String index = XMLUtil.getChildString(arch, "index").orElse(null);
            if (index == null)
            {
                logger.log(Level.WARNING, "<indexconfig> missing <archive> <index>");
                continue;
            }
            final File indexfile = new File(indexlist.getParentFile(), index);
            logger.log(Level.FINE, "Index: " + indexfile);
            if (! indexfile.canRead())
                logger.log(Level.WARNING, "Cannot read " + indexfile);
            archives.add(ArchiveFileReaderFactory.createReader(indexfile));
        }
    }

    @Override
    public String getDescription()
    {
        return "Channel Archiver";
    }

    @Override
    public Collection<String> getNamesByPattern(final String glob_pattern) throws Exception
    {
        // Search all sub-archives, but only return each name once.
        final Set<String> results = new HashSet<>();
        for (ArchiveReader reader : archives)
            results.addAll(reader.getNamesByPattern(glob_pattern));
        return results;
    }

    @Override
    public ValueIterator getRawValues(final String name, final Instant start, final Instant end)
            throws UnknownChannelException, Exception
    {
        final List<ValueIterator> readers = new ArrayList<>();
        for (ArchiveReader base : archives)
        {
            try
            {
                readers.add(base.getRawValues(name, start, end));
            }
            catch (UnknownChannelException ex)
            {
                // Skip base reader which doesn't include channel
            }
            catch (Exception ex)
            {
                throw ex;
            }
        }
        return new MergingValueIterator(readers.toArray(new ValueIterator[readers.size()]));
    }
}
