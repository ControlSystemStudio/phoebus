/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.phoebus.framework.util.ResourceParser.createAppURI;
import static org.phoebus.framework.util.ResourceParser.parseQueryArgs;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.csstudio.display.builder.model.DisplayModel;
import org.phoebus.framework.spi.AppInstance;
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
    public List<String> supportedFileExtentions()
    {
        return List.of(DisplayModel.FILE_EXTENSION, DisplayModel.LEGACY_FILE_EXTENSION);
    }

    @Override
    public AppInstance create()
    {
        throw new IllegalStateException("Display runtime always needs an input");
    }

    @Override
    public AppInstance create(final String resource)
    {
        // TODO Auto-generated method stub

        DisplayRuntime instance = null;

        final Map<String, List<String>> args = parseQueryArgs(createAppURI(resource));
        for (String file : args.get(ResourceParser.FILE_ARG))
        {
            final URL input = ResourceParser.createResourceURL(file);
            // Check for existing instance with that input
            // TODO input needs to include display path and macros
            final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, input);
            if (existing != null)
            {   // Found one, raise it
                instance = existing.getApplication();
                instance.raise();
            }
            else
            {   // Nothing found, create new one
                instance = new DisplayRuntime(this, input);
            }
        }

        return instance;
    }
}
