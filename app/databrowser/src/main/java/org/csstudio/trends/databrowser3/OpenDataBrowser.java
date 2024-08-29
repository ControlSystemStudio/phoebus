/*******************************************************************************
 * Copyright (c) 2018-2024 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;
import org.phoebus.ui.spi.ToolbarEntry;

import javafx.scene.image.Image;

/** Menu and toolbar entry for opening data browser
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenDataBrowser implements MenuEntry, ToolbarEntry
{
    @Override
    public String getName()
    {
        return Messages.DataBrowser;
    }

    @Override
    public String getMenuPath()
    {
        return Messages.DataBrowserMenuPath;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(getClass(), "/icons/databrowser.png");
    }

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(DataBrowserApp.NAME);
        return null;
    }
}
