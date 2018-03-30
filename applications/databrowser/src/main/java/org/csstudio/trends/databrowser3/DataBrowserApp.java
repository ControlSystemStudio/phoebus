/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.trends.databrowser3.model.PVItem;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockStage;

/** Application descriptor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataBrowserApp implements AppResourceDescriptor
{
    private static final List<String> FILE_EXTENSIONS = List.of("plt");
    public static final String NAME = "databrowser";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return Messages.DataBrowser;
    }

    @Override
    public URL getIconURL()
    {
        return getClass().getResource("/icons/databrowser.png");
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return FILE_EXTENSIONS;
    }

    @Override
    public DataBrowserInstance create()
    {
        return create(false);
    }

    /** @param minimal Should initial Perspective only show the plot?
     *  @return {@link DataBrowserInstance}
     */
    public DataBrowserInstance create(final boolean minimal)
    {
        return new DataBrowserInstance(this, minimal);
    }

    @Override
    public DataBrowserInstance create(final URI resource)
    {
        DataBrowserInstance instance = null;

        // Handles pv or file/http resource
        try
        {
            final List<String> pvs = ResourceParser.parsePVs(resource);
            if (pvs.size() > 0)
            {
                instance = create();
                for (String pv : pvs)
                    instance.getModel().addItem(new PVItem(pv, 0));
            }
            else
            {
                // Check for existing instance with that input
                final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, resource);
                if (existing != null)
                {   // Found one, raise it
                    instance = existing.getApplication();
                    instance.raise();
                }
                else
                {   // Nothing found, create new one
                    instance = create();
                    instance.loadResource(resource);
                }
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "PV Table cannot open '" + resource + "'", ex);
        }
        return instance;
    }
}
