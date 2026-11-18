/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import javafx.application.Platform;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Action that adds duplicate of PV to alarm tree configuration
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class DuplicatePVAction extends MenuItem
{
    /** @param node Node to position dialog
     *  @param model Model where new component is added
     *  @param original Item subject to copy
     */
    public DuplicatePVAction(final Node node, final AlarmClient model, final AlarmClientLeaf original)
    {
        super("Duplicate PV", ImageCache.getImageView(AlarmSystem.class, "/icons/move.png"));
        setOnAction(event ->
        {
            // Prompt for new PV name
            final RenameDialog dialog = new RenameDialog(model, original.getParent(), original);
            dialog.setHeaderText("Enter name for duplicate");
            DialogHelper.positionDialog(dialog, node, -100, -50);
            final String new_name = dialog.showAndWait().orElse(null);
            if (new_name == null)
                return;

            // Create near copy of original PV, but with new name
            final AlarmClientLeaf template = original.createDetachedCopy(new_name);

            // Request adding new PV
            JobManager.schedule(getText(), monitor -> {
                try {
                    final String new_path = AlarmTreePath.makePath(original.getParent().getPathName(), new_name);
                    model.sendItemConfigurationUpdate(new_path, template);
                } catch (Exception e) {
                    Logger.getLogger(DuplicatePVAction.class.getName()).log(Level.WARNING, "Failed to send item configuration", e);
                    Platform.runLater(() -> ExceptionDetailsErrorDialog.openError(e.getMessage(), e));
                }
            });
        });
    }
}
