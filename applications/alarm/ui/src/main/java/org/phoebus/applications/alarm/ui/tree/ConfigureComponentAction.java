/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;

/** Action for editing configuration of an item
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ConfigureComponentAction extends MenuItem
{
    /** @param node Node to position dialog
     *  @param model Model where new component is added
     *  @param item Parent item in alarm tree
     */
    public ConfigureComponentAction(final Node node, final AlarmClient model, final AlarmTreeItem<?> item)
    {
        super("Configure Item", ImageCache.getImageView(AlarmSystem.class, "/icons/configure.png"));
        setOnAction(event -> Platform.runLater(() ->
        {
            final ItemConfigDialog dialog = new ItemConfigDialog(model, item);
            DialogHelper.positionDialog(dialog, node, -250, -400);
            // Show dialog, not waiting for it to close with OK or Cancel
            dialog.show();
        }));
    }
}
