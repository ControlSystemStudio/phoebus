/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.imports;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** API for tool that imports data
 *  @author Kay Kasemir
 */
public class SampleImporters
{
    /** Map of file types to importers */
    private static Map<String, SampleImporter> importers = null;

    /** Locate all available importers */
    static
    {
        if (importers == null)
        {
            importers = new HashMap<String, SampleImporter>();

            // XXX Could use SPI to locate importers
            // For now there's only one
            final SampleImporter importer = new CSVSampleImporter();
            importers.put(importer.getType(), importer);
        }
    }

    /** Prevent instantiation */
    private SampleImporters()
    {
    }

    /** @return Array of supported types
     *  @throws Exception on error initializing available importers
     */
    public static Collection<String> getTypes() throws Exception
    {
        return importers.keySet();
    }


    /** Obtain sample importer
     *  @param type
     *  @return {@link SampleImporterInfo} for that type or <code>null</code> if not known
     */
    public static SampleImporter getImporter(final String type)
    {
        return importers.get(type);
    }
}
