/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.imports;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;

/** Factory for {@link ArchiveReader} that imports data from file
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImportArchiveReaderFactory implements ArchiveReaderFactory
{
    /** Prefix used by this reader */
    final public static String PREFIX = "import:";

    /** Map URLs to ImportArchiveReader for the URL
     *
     *  <p>The reader will be invoked whenever data for a new time range is
     *  requested.
     *
     *  <p>Since the underlying file doesn't change, cache the readers by URL,
     *  and the reader will only parse the file once, then always return
     *  the remembered values.
     *
     *  <p>To prevent running out of memory, Model#stop() will
     *  remove cache data for items in model.
     */
    final private static ConcurrentHashMap<String, ArchiveReader> cache = new ConcurrentHashMap<>();

    @Override
    public String getPrefix()
    {
        return PREFIX;
    }

    /** Create 'URL'
     *  @param type File type
     *  @param path Path to file
     *  @return "import:csv:/path/to/file.abc"
     */
    public static String createURL(final String type, final String path)
    {
        return PREFIX + type + ":" + path;
    }

    /** Parse URL
     *  @param url Import URL
     *  @return String[] with type, path
     *  @throws Exception if URL doesn't parse
     */
    public static String[] parseURL(final String url) throws Exception
    {
        final int prefix_len = PREFIX.length();
        if (! url.startsWith(PREFIX))
            throw new Exception("URL does not start with '" + PREFIX + "': " + url);
        final int sep = url.indexOf(":", prefix_len);
        if (sep < 0)
            throw new Exception("Missing import data type from '" + url + "'");
        final String type = url.substring(prefix_len, sep);
        final String path = url.substring(sep+1);
        return new String[] { type, path };
    }

    @Override
    public ArchiveReader createReader(final String url) throws Exception
    {
        try
        {
            return cache.computeIfAbsent(url, this::doCreateReader);
        }
        catch (Error ex)
        {
            throw new Exception(ex.getCause());
        }
    }

    public ArchiveReader doCreateReader(final String url)
    {
        // Get path, importer from URL
        try
        {
            final String[] type_path = parseURL(url);
            final String type = type_path[0];
            final String path = type_path[1];
            final SampleImporter importer = SampleImporters.getImporter(type);
            if (importer == null)
                throw new Exception("Unknown import data type " + type);
            return new ImportArchiveReader(path, importer);
        }
        catch (Exception ex)
        {
            throw new Error(ex);
        }
    }

    /** Removed cached data for given archive data sources
     *  @param sources {@link ArchiveDataSource}[]
     */
    public static void removeCachedArchives(final Collection<ArchiveDataSource> sources)
    {
        for (ArchiveDataSource source : sources)
            cache.remove(source.getUrl());
    }
}
