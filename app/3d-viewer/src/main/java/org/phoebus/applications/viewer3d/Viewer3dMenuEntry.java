/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.viewer3d;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/** Menu entry
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class Viewer3dMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return Viewer3dApp.DISPLAY_NAME;
    }

    @Override
    public String getMenuPath()
    {
        return "Display";
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(Viewer3dPane.class, "/icons/viewer3d.png");
    }

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(Viewer3dApp.NAME);
        return null;
    }
}
