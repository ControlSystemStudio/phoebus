/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import java.net.URI;
import java.net.URL;
import java.util.List;

import org.phoebus.framework.spi.AppResourceDescriptor;

public class DataBrowserApp implements AppResourceDescriptor
{
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
        return List.of("plt");
    }

    @Override
    public DataBrowserInstance create()
    {
        return new DataBrowserInstance(this);
    }

    @Override
    public DataBrowserInstance create(URI resource)
    {
        final DataBrowserInstance db = create();
        // TODO db.load(resource);
        return db;
    }
}
