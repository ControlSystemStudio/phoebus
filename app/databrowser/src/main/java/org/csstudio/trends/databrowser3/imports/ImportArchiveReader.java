/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.imports;

import java.io.FileInputStream;
import java.time.Instant;
import java.util.List;

import org.epics.vtype.VType;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;

/** Archive reader that imports data from a file
 *
 *  <p>Performs the import once, reading the complete file.
 *  From then on, reading the 'archive' returns
 *  the same samples.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImportArchiveReader implements ArchiveReader
{
    final private String path;
    final private SampleImporter importer;
    private List<VType> values = null;

    public ImportArchiveReader(final String path, final SampleImporter importer)
    {
        this.path = path;
        this.importer = importer;
    }

    @Override
    public String getDescription()
    {
        return "Imported " + importer.getType();
    }

    @Override
    public List<String> getNamesByPattern(final String glob_pattern)
            throws Exception
    {
        return List.of();
    }

    @Override
    public ValueIterator getRawValues(final String name, final Instant start,
                                      final Instant end) throws UnknownChannelException, Exception
    {
        if (values == null)
        {
            // Import data
            values = importer.importValues(new FileInputStream(path));
        }
        return new ArrayValueIterator(values);
    }
}
