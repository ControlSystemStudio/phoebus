/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import org.phoebus.ui.application.Messages;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

/** Menu item to name a {@link DockPane}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NamePaneMenuItem extends MenuItem
{
    public NamePaneMenuItem(final DockPane pane)
    {
        super(Messages.NamePane, ImageCache.getImageView(DockItem.class, "/icons/name.png"));
        setOnAction(event -> editName(pane));
    }

    /** Edit name of dock pane */
    private void editName(final DockPane pane)
    {
        final TextInputDialog dialog = new TextInputDialog(pane.getName());
        dialog.setTitle(Messages.NamePane);
        dialog.setHeaderText(Messages.NamePaneHdr);
        DialogHelper.positionDialog(dialog, pane, -200, -100);
        dialog.showAndWait().ifPresent(pane::setName);
    }
}
