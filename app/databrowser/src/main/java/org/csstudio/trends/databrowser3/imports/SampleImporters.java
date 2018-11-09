/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.imports;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;

/** API for tool that imports data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SampleImporters
{
    /** Map of file types to importers */
    private static final Map<String, SampleImporter> importers = new HashMap<>();

    /** Locate all available importers */
    static
    {
        try
        {
            for (SampleImporter importer : ServiceLoader.load(SampleImporter.class))
            {
                logger.log(Level.CONFIG, "SampleImporter for '" + importer.getType() + "'");
                importers.put(importer.getType(), importer);
            }
        }
        catch (Throwable ex)
        {
            logger.log(Level.SEVERE, "Cannot locate SampleImporters", ex);
        }
    }

    /** Prevent instantiation */
    private SampleImporters()
    {
    }

    /** @return Array of supported types */
    public static Collection<String> getTypes()
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
