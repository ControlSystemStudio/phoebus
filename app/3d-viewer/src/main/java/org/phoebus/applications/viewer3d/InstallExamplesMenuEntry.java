/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.viewer3d;

import org.phoebus.ui.examples.ExampleInstaller;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/** Menu entry
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class InstallExamplesMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return "Install Example 3D Viewer Files";
    }

    @Override
    public String getMenuPath()
    {
        return "Display.Examples";
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(Viewer3dPane.class, "/icons/viewer3d.png");
    }

    @Override
    public Void call() throws Exception
    {
        new ExampleInstaller("Select Directory for Installing 3D Viewer Examples",
                Viewer3dApp.class.getResource("/3d_viewer_examples"),
                "3D Viewer",
                "NaCl.shp",
                Viewer3dApp.NAME,
                "Examples have been installed in\n\n  {0}")
           .call();

           return null;
    }
}
