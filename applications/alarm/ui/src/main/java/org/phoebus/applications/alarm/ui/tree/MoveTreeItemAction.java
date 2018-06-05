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
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeView;

/** Action to move a PV
 *  @author Evan Smith
 */
public class MoveTreeItemAction extends MenuItem 
{

	/**
	 * <p> Constructor for MoveTreeItemAction. Sets onAction to move the passed item within the passed tree view.
	 * @param node The TreeView.
	 * @param model The AlarmClient.
	 * @param item The item within the TreeView to move.
	 */
	public MoveTreeItemAction(TreeView<AlarmTreeItem<?>> node, 
							   AlarmClient model, 
							   AlarmTreeItem<?> item)
	{
		super("Move Item", ImageCache.getImageView(AlarmSystem.class, "/icons/move.png"));
		
		setOnAction(event ->
    	{
    		//Prompt for new name
        	String new_path = null;
        	while (new_path != null && ! AlarmTreeHelper.validatePath(new_path, node.getRoot().getValue()))
        	{
    			new_path = AlarmTreeHelper.prompt(getText(), "Enter new name for Component", item.getPathName(), node);
    			if (null == new_path)
    				return;
        	}
        	
        	System.out.println(new_path);
        	/*
    		// Tree view keeps the selection indices, which will point to wrong content
            // after those items have been removed.
            if (node instanceof TreeView<?>)
                ((TreeView<?>) node).getSelectionModel().clearSelection();

			/*
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
            */
    	});
	}

}
