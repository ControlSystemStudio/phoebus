/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import org.csstudio.display.builder.runtime.Messages;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Action to show/hide toolbar
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayToolbarAction extends MenuItem
{
    private static final Image icon = ImageCache.getImage(DisplayRuntimeInstance.class, "/icons/display_toolbar.png");

    DisplayToolbarAction(final DisplayRuntimeInstance instance)
    {
        super(instance.isToolbarVisible() ? Messages.Toolbar_Hide : Messages.Toolbar_Show,
              new ImageView(icon));
        setOnAction(event -> toggleToolbar(instance));
    }

    private void toggleToolbar(final DisplayRuntimeInstance instance)
    {
        instance.showToolbar(! instance.isToolbarVisible());
        setText(instance.isToolbarVisible() ? Messages.Toolbar_Hide : Messages.Toolbar_Show);
    }
}
