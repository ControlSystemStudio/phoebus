/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;
import static org.phoebus.framework.util.ResourceParser.createAppURI;
import static org.phoebus.framework.util.ResourceParser.parseQueryArgs;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.macros.Macros;
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
    public DisplayRuntimeInstance create()
    {
        return new DisplayRuntimeInstance(this);
    }

    @Override
    public DisplayRuntimeInstance create(final String resource)
    {
        // Expect "display_runtime?file=file:/some/path&MACRO=Some+Value&X=2"
        if (! resource.startsWith("display_runtime?file="))
        {
            logger.log(Level.SEVERE, "Expected '\"display_runtime?file=...\", ignoring " + resource);
            return null;
        }

        // Get file path and maybe macros
        final Map<String, List<String>> args = parseQueryArgs(createAppURI(resource));
        String path = null;
        final Macros macros = new Macros();
        for (Map.Entry<String, List<String>> entry : args.entrySet())
        {
            if (entry.getValue().size() != 1)
                logger.log(Level.WARNING, "Expected 1 value for '" + entry.getKey() + "' but got " + entry.getValue());
            else if (entry.getKey().equals(ResourceParser.FILE_ARG))
                path = entry.getValue().get(0);
            else
                macros.add(entry.getKey(),  entry.getValue().get(0));
        }
        if (path == null)
        {
            logger.log(Level.WARNING, "Missing 'file=..' in " + resource);
            return null;
        }

        // Create display info
        final DisplayInfo info = new DisplayInfo(path, null, macros, true);
        // Display URL, normalized such that for example macros are alphabetically sorted,
        // to uniquely identify an already running instance via its input
        final URL input = info.toURL();

        // Check for existing instance with that input, i.e. path & macros
        final DisplayRuntimeInstance instance;
        final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, input);
        if (existing != null)
        {   // Found one, raise it
            instance = existing.getApplication();
            instance.raise();
        }
        else
        {   // Nothing found, create new one
            instance = create();
            instance.loadDisplayFile(info);
        }

        return instance;
    }
}
