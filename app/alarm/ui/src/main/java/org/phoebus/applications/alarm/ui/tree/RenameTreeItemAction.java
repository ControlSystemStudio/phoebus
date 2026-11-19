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
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeView;

/** Action to rename a PV
 *  @author Kay Kasemir
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
class RenameTreeItemAction extends MenuItem
{
    /** @param node UI node used to position dialog
     *  @param model {@link AlarmClient}
     *  @param pv PV to rename
     */
    public RenameTreeItemAction(final TreeView<AlarmTreeItem<?>> node,
                          final AlarmClient model,
                          final AlarmTreeItem<?> item)
    {
        super(Messages.renameItem, ImageCache.getImageView(AlarmSystem.class, "/icons/rename.png"));

		setOnAction(event ->
		{
			// Prompt for new name
			final RenameDialog dialog = new RenameDialog(model, item.getParent(), item);
			dialog.setHeaderText(Messages.enterNewName);
			DialogHelper.positionDialog(dialog, node, -100, -50);
			final String new_name = dialog.showAndWait().orElse(null);
			if (new_name == null)
				return;

			// Delete old, rebuild child links, and add new name in background thread.
			JobManager.schedule(getText(), monitor ->
			{
				final AlarmTreeItem<BasicState> parent = item.getParent();
				try {
					final String new_path = AlarmTreePath.makePath(parent.getPathName(), new_name);
					// Add the new item
					model.sendItemConfigurationUpdate(new_path, item);
					// ...and rebuild all its children
					if (item instanceof AlarmClientNode) {
						AlarmTreeHelper.rebuildTree(model, item, new_path);
					}
					// Remove the item and all its children
					model.removeComponent(item);
				} catch (final Exception ex) {
					ExceptionDetailsErrorDialog.openError(Messages.error,
							Messages.renameItemFailed,
							ex);
				}
			});
		});
    }
}
