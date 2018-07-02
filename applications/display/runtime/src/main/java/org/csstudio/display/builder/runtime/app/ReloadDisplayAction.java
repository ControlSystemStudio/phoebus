/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import org.csstudio.display.builder.runtime.Messages;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Action to re-load current display
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ReloadDisplayAction extends MenuItem
{
    private static final Image icon = ImageCache.getImage(DockStage.class, "/icons/refresh.png");

    ReloadDisplayAction(final DisplayRuntimeInstance instance)
    {
        super(Messages.ReloadDisplay, new ImageView(icon));
        setOnAction(event -> instance.reload());
    }
}
