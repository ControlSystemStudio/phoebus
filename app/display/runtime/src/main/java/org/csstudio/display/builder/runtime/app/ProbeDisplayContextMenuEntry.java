/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.net.URI;
import java.util.List;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.runtime.Preferences;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.image.Image;

/** "Probe Display" context menu entry for PVs
 *
 *  <p>Opens with macro PV set based on selection.
 *  Display to open is set by preference.
 *  When empty, context menu is not shown.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ProbeDisplayContextMenuEntry implements ContextMenuEntry
{
    private static final Class<?> type;

    static
    {   // Enable/disable the context menu entry by associating it with PV or nothing
        type = Preferences.probe_display.isBlank()
              ? null
              : ProcessVariable.class;
    }

    @Override
    public String getName()
    {
        return "Probe Display";
    }

    @Override
    public Class<?> getSupportedType()
    {
        return type;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(DisplayModel.class, "/icons/runtime.png");
    }

    @Override
    public void call(final Selection selection) throws Exception
    {
        final List<ProcessVariable> pvs = selection.getSelections();
        for (ProcessVariable pv : pvs)
        {
            // Open the probe display for each PV, passed via $(PV) macro
            final URI resource;
            final String encoded = ResourceParser.encode(pv.getName());
            if (Preferences.probe_display.contains("?"))
                resource = URI.create(Preferences.probe_display + "&PV=" + encoded);
            else
                resource = URI.create(Preferences.probe_display + "?PV=" + encoded);
            ApplicationService.createInstance(DisplayRuntimeApplication.NAME, resource);
        }
    }
}
