/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser;

import javafx.scene.image.Image;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

/**
 * A menu item under the "Display" menu which launches the Chart Browser application.
 */
public class ChartBrowserMenuEntry implements MenuEntry {
    /**
     * {@inheritDoc}
     *
     * @return the display name of the Chart Browser app
     */
    @Override
    public String getName() {
        return ChartBrowserApp.DISPLAYNAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the icon to show next to the Chart Browser menu item
     */
    @Override
    public Image getIcon() {
        return ImageCache.getImage(ChartBrowserApp.class, "/icons/chartbrowser.png");
    }

    /**
     * {@inheritDoc}
     *
     * Creates a new instance of the Chart Browser application when selected
     * from the menu.
     *
     */
    @Override
    public Void call() {
        ApplicationService.createInstance(ChartBrowserApp.NAME);
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return the parent menu path under which this entry appears
     */
    @Override
    public String getMenuPath() {
        return "Display";
    }
}
