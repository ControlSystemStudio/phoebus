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
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

/** Action that adds duplicate of PV to alarm tree configuration
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class DuplicatePVAction extends MenuItem
{
    /** @param node Node to position dialog
     *  @param model Model where new component is added
     *  @param parent Parent item in alarm tree
     */
    public DuplicatePVAction(final Node node, final AlarmClient model, final AlarmClientLeaf original)
    {
        super("Duplicate PV", ImageCache.getImageView(AlarmSystem.class, "/icons/move.png"));
        setOnAction(event ->
        {
            // Prompt for new PV name
            final TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle(getText());
            dialog.setHeaderText("Enter name for duplicate of\n" + original.getPathName());
            DialogHelper.positionDialog(dialog, node, -100, -50);
            final String new_name = dialog.showAndWait().orElse(null);
            if (new_name == null  ||  new_name.isEmpty())
                return;

            // Create near copy of original PV, but with new name
            final AlarmClientLeaf template = new AlarmClientLeaf(null, new_name);
            template.setDescription(original.getDescription());
            template.setEnabled(original.isEnabled());
            template.setLatching(original.isLatching());
            template.setAnnunciating(original.isAnnunciating());
            template.setDelay(original.getDelay());
            template.setCount(original.getCount());
            template.setFilter(original.getFilter());
            template.setGuidance(original.getGuidance());
            template.setDisplays(original.getDisplays());
            template.setCommands(original.getCommands());
            template.setActions(original.getActions());

            // Request adding new PV
            final String new_path = AlarmTreePath.makePath(original.getParent().getPathName(), new_name);
            JobManager.schedule(getText(), monitor -> model.sendItemConfigurationUpdate(new_path, template));
        });
    }
}
