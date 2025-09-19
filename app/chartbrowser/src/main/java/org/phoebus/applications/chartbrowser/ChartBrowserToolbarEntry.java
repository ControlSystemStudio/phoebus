/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.ToolbarEntry;

/**
 * A toolbar button entry which launches the Chart Browser application.
 */
public class ChartBrowserToolbarEntry implements ToolbarEntry {

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
     * Creates a new instance of the Chart Browser application when the toolbar
     * button is pressed.
     */
    @Override
    public Void call() {
        ApplicationService.createInstance(ChartBrowserApp.NAME);
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return a unique identifier for this toolbar entry
     */
    @Override
    public String getId() {
        return "chartbrowser";
    }
}
