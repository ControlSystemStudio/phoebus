/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.net.URI;
import java.net.URL;
import java.util.List;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.ModelPlugin;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockStage;

/** Display Runtime Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayRuntimeApplication implements AppResourceDescriptor
{
    public static final String NAME = "display_runtime";
    public static final String DISPLAY_NAME = "Display Runtime";

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
        return DisplayModel.class.getResource("/icons/runtime.png");
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return DisplayModel.FILE_EXTENSIONS;
    }

    @Override
    public void start()
    {
        ModelPlugin.reloadConfigurationFiles();
    }

    @Override
    public DisplayRuntimeInstance create()
    {
        return new DisplayRuntimeInstance(this);
    }

    @Override
    public DisplayRuntimeInstance create(final URI resource)
    {
        // Create display info
        final DisplayInfo info = DisplayInfo.forURI(resource);

        // Convert back into URI
        // Content should be very similar, but normalized such that for example macros
        // are alphabetically sorted to uniquely identify an already running instance
        // via its input
        final URI input = info.toURI();

        // Check for existing instance with that input, i.e. path & macros
        final DisplayRuntimeInstance instance;
        final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, input);
        if (existing != null)
        {   // Found one, raise it
            instance = existing.getApplication();
            instance.raise();

            // Re-load because
            // a) Editor re-opened this for a file that was just changed
            // b) Somebody decided to open it again to force a re-load
            // c) Somebody forgot where the panel is, opened it again,
            //    so re-load isn't necessary, but also doesn't hurt?
            instance.reload();
        }
        else
        {   // Nothing found, create new one
            final String pane = ResourceParser.getTargetName(resource);
            instance = new DisplayRuntimeInstance(this, pane);
            instance.loadDisplayFile(info);
        }

        return instance;
    }
}
