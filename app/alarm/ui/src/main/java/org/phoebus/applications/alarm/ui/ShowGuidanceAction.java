/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;

/** Action that displays an AlarmTreeItem's guidance
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ShowGuidanceAction extends MenuItem
{
    /** @param node Node to position dialog
     *  @param item Alarm item
     *  @param guidance Info to show
     */
    public ShowGuidanceAction(final Node node, final AlarmTreeItem<?> item, final TitleDetail guidance)
    {
        super(guidance.title, ImageCache.getImageView(ImageCache.class, "/icons/info.png"));
        setOnAction(event ->
        {
            final Alert dialog = new Alert(AlertType.INFORMATION);
            dialog.setTitle("Guidance for " + item.getName());
            dialog.setHeaderText(guidance.title);

            final TextArea details = new TextArea(guidance.detail);
            details.setEditable(false);
            details.setWrapText(true);

            dialog.getDialogPane().setContent(details);

            DialogHelper.positionDialog(dialog, node, -100, -50);
            dialog.setResizable(true);
            dialog.showAndWait();
        });
    }
}
