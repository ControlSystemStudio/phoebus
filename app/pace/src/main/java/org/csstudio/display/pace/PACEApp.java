/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.pace;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockStage;

/** Application descriptor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PACEApp implements AppResourceDescriptor
{
    /** PACE logger */
    public static final Logger logger = Logger.getLogger(PACEApp.class.getPackageName());

    public static final String NAME = "pace";
    public static final String DISPLAY_NAME = "PACE";
    public static final String EXTENSION = "pace";
    private static final List<String> extensions = List.of(EXTENSION);

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
        return PACEApp.class.getResource("/icons/pace.png");
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return extensions;
    }

    @Override
    public AppInstance create()
    {
        throw new IllegalStateException("PACE instance must be created with *.pace resource");
    }

    @Override
    public AppInstance create(final URI resource)
    {
        final PACEInstance instance;
        final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, resource);
        if (existing != null)
        {
            instance = existing.getApplication();
            instance.raise();
        }
        else
            instance =  new PACEInstance(this, resource);
        return instance;
    }
}
