/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.RTPlot;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Menu item to show/hide toolbar
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ToggleToolbarMenuItem extends MenuItem
{
    /** @param plot Plot on which to toggle the toolbar */
    public ToggleToolbarMenuItem(final RTPlot<?> plot)
    {
        setGraphic(ImageCache.getImageView(RTPlot.class, "/icons/toolbar.png"));

        if (plot.isToolbarVisible())
            setText(Messages.Toolbar_Hide);
        else
            setText(Messages.Toolbar_Show);
        setOnAction(event  ->  plot.showToolbar(! plot.isToolbarVisible()));
    }
}
