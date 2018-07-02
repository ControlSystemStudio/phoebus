/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import java.util.List;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeView;

/** @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmTreeHelper
{
	/** Rebuild the tree structure by recreating the parents' children at the new path location.
	 *  @param model AlarmClient used to communicate to the AlarmServer
	 *  @param parent AlarmTreeItem whose children will be re-linked to the new path location.
	 *  @param path Path that the parents' children will now be located at.
	 *  @throws Exception
	 */
	public static void rebuildTree(AlarmClient model, AlarmTreeItem<?> parent, String path) throws Exception
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

	/** Prompt the screen with a dialog box.
	 *  @param title Title string of the dialog box.
	 *  @param header Header string of the dialog box.
	 *  @param default_text Default text contents of dialog box.
	 *  @param node Node where the dialog box will be located.
	 *  @return String The entered string or null if the entry was empty.
	 */
    public static String prompt(String title, String header,  final String default_text, final TreeView<AlarmTreeItem<?>> node)
    {
    	// Prompt for new name
        final TextInputDialog prompt = new TextInputDialog(default_text);
        DialogHelper.positionDialog(prompt, node, -200, -100);
        prompt.setTitle(title);
        prompt.setHeaderText(header);
        prompt.getDialogPane().setPrefWidth(500);
        prompt.setResizable(true);
        final String input = prompt.showAndWait().orElse("");
        if (input.isEmpty())
            return null;
        return input;
    }

	/** Validate the passed path string by checking if the path name is valid.
	 *
	 *  <p> A path name is considered valid if the path to the item's new location exists in the tree,
	 *  and if the items new location is not a PV.
	 *
	 *  <p> For example: If "top/middle/bottom/to_move" wanted to be moved to "top/middle/to_move".
	 *  The path "top/middle" must be in the tree and middle must not be a PV.
	 *
	 *  @param path The path to the new location.
	 *  @param root The root node of the AlarmTree
	 *  @return <code>true</code> if the pathname is valid, and if the path to the new location exists in the tree.
	 */
	public static boolean validateNewPath(String path, AlarmTreeItem<?> root)
	{
	    if (null == path || path.isEmpty())
	        return false;

		String[] path_elems = AlarmTreePath.splitPath(path);
		// Make sure the path exists
		// The proposed parent must exist. path_elems includes the new addition as well so only check length-1.
		int elem_num = path_elems.length - 1;
		AlarmTreeItem<?> item = root;

		// New addition must be above root level. Tree only displays what's on top of root area.
		if (! (path_elems.length > 0) || ! (root.getName().equals(path_elems[0])))
		    return false;

		for (int i = 1; i < elem_num; i++)
		{
		    item = item.getChild(path_elems[i]);
		    if (null == item)
		    {
		        // System.out.println("Path element " + path_elems[i] + " does not exist in the tree at that location.");
		        return false;
		    }
		    // Make sure the path does not contain a PV.
		    // PV cannot have children.
		    if (item instanceof AlarmClientLeaf)
		    {
		        // System.out.println("Path element " + path_elems[i] + " is a leaf.");
		        return false;
		    }
		}

		return true;
	}
}
