/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.viewer3d;

import java.net.URI;
import java.net.URL;
import java.util.List;

import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockStage;

/** Application descriptor
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class Viewer3dApp implements AppResourceDescriptor
{
    public static final String NAME = "3d_viewer";

    public static final String DISPLAY_NAME = "3D Viewer";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public URL getIconURL()
    {
        return getClass().getResource("/icons/viewer3d.png");
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return Viewer3dPane.FILE_EXTENSIONS;
    }

    @Override
    public AppInstance create()
    {
        return new Viewer3dInstance(this, null);
    }

    @Override
    public AppInstance create(final URI resource)
    {
        // Check for existing instance with that input, i.e. path & macros
        final Viewer3dInstance instance;
        final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, resource);
        if (existing != null)
        {   // Found one, raise it
            instance = existing.getApplication();
            instance.raise();

            // Reload the resource.
            instance.reload();
        }
        else
        {   // Nothing found, create new one
            instance = new Viewer3dInstance(this, resource);
        }

        return instance;
    }
}
