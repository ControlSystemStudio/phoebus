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
import org.phoebus.framework.jobs.JobManager;
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
        super("Rename Item", ImageCache.getImageView(AlarmSystem.class, "/icons/rename.png"));

        if (item instanceof AlarmClientNode)
        {
        	setOnAction(event ->
        	{
        		//Prompt for new name
	        	final String new_name = AlarmTreeHelper.prompt(getText(), "Enter new name for Node", item.getName(), node);
	        	if (null == new_name)
	        		return;
	        	// Tree view keeps the selection indices, which will point to wrong content
	            // after those items have been removed
	            if (node instanceof TreeView<?>)
	                ((TreeView<?>) node).getSelectionModel().clearSelection();

	            // Delete old, rebuild child links, and add new name in background thread.
	            JobManager.schedule(getText(), monitor ->
	            {
	                final AlarmTreeItem<BasicState> parent = item.getParent();
	                // Remove the item and all its children.
	                // Add the new item, and then rebuild all its children.
	                final String new_path = AlarmTreePath.makePath(parent.getPathName(), new_name);
					model.sendItemConfigurationUpdate(new_path, item);
	                AlarmTreeHelper.rebuildTree(model, item, new_path);
	                model.removeComponent(item);
	            });
        	});
        }
        else
        {
	        setOnAction(event ->
	        {
	        	//Prompt for new name
	        	final String new_name = AlarmTreeHelper.prompt(getText(), "Enter new name for PV", item.getName(), node);
	        	if (null == new_name)
	        		return;
	        	// Tree view keeps the selection indices, which will point to wrong content
	            // after those items have been removed
	            if (node instanceof TreeView<?>)
	                ((TreeView<?>) node).getSelectionModel().clearSelection();

	            // Delete old, add new name in background thread
	            JobManager.schedule(getText(), monitor ->
	            {
	                final AlarmTreeItem<BasicState> parent = item.getParent();
	                final String new_path = AlarmTreePath.makePath(parent.getPathName(), new_name);
	                model.sendItemConfigurationUpdate(new_path, item);
	                model.removeComponent(item);
	            });
	        });
        }
    }


}
