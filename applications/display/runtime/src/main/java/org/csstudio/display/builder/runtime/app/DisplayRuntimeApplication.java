/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
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
        DisplayRuntimeInstance instance = null;

        // Expect "display_runtime?file=file:/some/path;MACRO=Some+Value;X=2"
        if (! resource.startsWith("display_runtime?file="))
        {
            logger.log(Level.SEVERE, "Expected '\"display_runtime?file=...\", ignoring " + resource);
            return null;
        }

        // "file:/some/path;MACRO=Some+Value;X=2"
        final URL orig_input = ResourceParser.createResourceURL(resource.substring(21));

        // Convert URL to DisplayInfo and back for normalized URL,
        // where for example macros are alphabetically sorted,
        // to uniquely identify an already running instance via its input
        final DisplayInfo info = DisplayInfo.forURL(orig_input);
        final URL input = info.toURL();

        // Check for existing instance with that input, i.e. path & macros
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
