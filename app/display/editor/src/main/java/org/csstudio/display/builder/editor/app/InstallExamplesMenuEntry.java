/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.app;

import org.csstudio.display.builder.model.DisplayModel;
import org.phoebus.ui.examples.ExampleInstaller;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/** Menu entry for installing example displays
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class InstallExamplesMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return "Install Example Displays";
    }

    @Override
    public String getMenuPath()
    {
        return "Display.Examples";
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(DisplayModel.class, "/icons/display.png");
    }

    @Override
    public Void call() throws Exception
    {
        new ExampleInstaller("Select Directory for Installing Display Builder Examples",
             DisplayModel.class.getResource("/examples"),
             "Display Builder",
             "01_main.bob",
             "display_runtime",
             "Examples have been installed in\n\n  {0}\n\nYou can open them via File/Open to execute or edit.")
        .call();

        return null;
    }
}
