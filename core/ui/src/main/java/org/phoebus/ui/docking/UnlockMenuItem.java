/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import org.phoebus.ui.application.Messages;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Menu item to un-lock {@link DockPane}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class UnlockMenuItem extends MenuItem
{
    public UnlockMenuItem(final DockPane pane)
    {
        super(Messages.UnLockPane, ImageCache.getImageView(DockItem.class, "/icons/unlock.png"));
        setOnAction(event -> pane.setFixed(false));
    }
}
