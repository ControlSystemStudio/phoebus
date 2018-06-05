/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import java.util.List;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeView;

/** Action to rename a PV
 *  @author Kay Kasemir
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

        // TODO Should be able to rename any item, not just a leaf,
        //      by renaming that item and all its child entries
        if (item instanceof AlarmClientNode)
        {
        	setOnAction(event ->
        	{
        		//Prompt for new name
	        	final String new_name = prompt("Enter new name for Component", node, item);
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
	                rebuildTree(model, item, new_path);
	                model.removeComponent(item);
	            });
        	});
        }
        else
        {
	        setOnAction(event ->
	        {
	        	//Prompt for new name
	        	final String new_name = prompt("Enter new name for PV", node, item);
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

	private void rebuildTree(AlarmClient model, AlarmTreeItem<?> parent, String path) throws Exception
	{
		if (null == model ||
			null == parent ||
			null == path ||
			path.isEmpty())
			return;

		// Recreate every child. Each child provides the content for recreation.
		// There is no need to check if the child is a leaf or node because the
		// item configuration update takes the child as an argument. Whatever it
		// is, is how it will be recreated.
		final List<AlarmTreeItem<?>> children = parent.getChildren();
		for (final AlarmTreeItem<?> child : children)
		{
			final String new_path = AlarmTreePath.makePath(path, child.getName());
			model.sendItemConfigurationUpdate(new_path, child);
			rebuildTree(model, child, new_path);
		}
	}

	private String prompt(String text, final TreeView<AlarmTreeItem<?>> node, final AlarmTreeItem<?> item)
    {
    	// Prompt for new name
        final TextInputDialog prompt = new TextInputDialog(item.getName());
        DialogHelper.positionDialog(prompt, node, -200, -100);
        prompt.setTitle(getText());
        prompt.setHeaderText(text);
        final String new_name = prompt.showAndWait().orElse(null);
        if (new_name == null || new_name.isEmpty())
            return null;
        return new_name;
    }
}
