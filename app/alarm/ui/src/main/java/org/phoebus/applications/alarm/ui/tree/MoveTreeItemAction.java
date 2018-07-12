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
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeView;

/** Action to move an {@link AlarmTreeItem} to a new location in an AlarmTree.
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class MoveTreeItemAction extends MenuItem
{
	/** Constructor for MoveTreeItemAction
	 *
	 *  <p>Sets onAction to move the passed item within the passed tree view.
	 *  @param node The TreeView.
	 *  @param model The AlarmClient.
	 *  @param item The item within the TreeView to move.
	 */
    public MoveTreeItemAction(TreeView<AlarmTreeItem<?>> node,
							   AlarmClient model,
							   AlarmTreeItem<?> item)
	{
		super("Move Item", ImageCache.getImageView(AlarmSystem.class, "/icons/move.png"));

		setOnAction(event ->
    	{
    		//Prompt for new name

    	    String prompt = "Enter new path for item";

        	String path = item.getPathName();
        	while (true)
        	{
    			path = AlarmTreeHelper.prompt(getText(), prompt, path, node);
    			if (path == null)
    			    return;
    			if (AlarmTreeHelper.validateNewPath(path, node.getRoot().getValue()) )
    			    break;
    			prompt = "Invalid path. Try again or cancel";
        	}

    		// Tree view keeps the selection indices, which will point to wrong content
            // after those items have been removed.
            if (node instanceof TreeView<?>)
                ((TreeView<?>) node).getSelectionModel().clearSelection();

			final String new_path = path;
			// On a background thread, send the item configuration updates for the item to be moved and all its children.
            JobManager.schedule(getText(), monitor ->
            {
                // Move the item.
                model.sendItemConfigurationUpdate(new_path, item);
                // Move the item's children.
                AlarmTreeHelper.rebuildTree(model, item, new_path);
                // Delete the old item. This deletes the old item's children as well.
                model.removeComponent(item);
            });

    	});
	}

}
