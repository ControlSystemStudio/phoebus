/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.console;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/** Menu entry to open console app
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenConsole implements MenuEntry
{
    @Override
    public String getName()
    {
        return ConsoleApp.DISPLAY_NAME;
    }

    @Override
    public String getMenuPath()
    {
        return "Utility";
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(ConsoleApp.class, "/icons/console_view.png");
    }

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(ConsoleApp.NAME);
        return null;
    }
}
