/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeView;
import org.phoebus.util.time.TimestampFormats;

/** @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmTreeHelper
{
	/** Rebuild the tree structure by recreating the parents' children at the new path location.
	 *  @param model AlarmClient used to communicate to the AlarmServer
	 *  @param parent AlarmTreeItem whose children will be re-linked to the new path location.
	 *  @param path Path that the parents' children will now be located at.
	 *  @throws Exception on error
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
		        return false;
		    }
		    // Make sure the path does not contain a PV.
		    // PV cannot have children.
		    if (item instanceof AlarmClientLeaf)
		    {
		        return false;
		    }
		}

		return true;
	}

	/**
	 * Collects {@link AlarmClientLeaf}s items.
	 * @param items A {@link List} of {@link AlarmTreeItem}s, typically selected by user in the tree view. This could
	 *              be a mix of leaf and non-leaf nodes. Moreover, leaf nodes could be child nodes of non-leaf nodes
	 *              in the {@link List}.
	 * @return A {@link Set} of only {@link AlarmClientLeaf}s, i.e. no duplicates even id user selection would indicate it.
	 */
	protected static Set<AlarmClientLeaf> getLeafItems(List<AlarmTreeItem<?>> items){
		return items.stream().flatMap(item -> streamLeafItems(item)).collect(Collectors.toSet());
	}

	/**
	 * Collects {@link AlarmClientLeaf}s items.
	 * @param root The start node from where to get {@link AlarmClientLeaf}s. If this is an {@link AlarmClientLeaf}, it
	 *             will be returned as the sole item in the {@link Set}
	 * @return A {@link Set} of only {@link AlarmClientLeaf}s.
	 */
	protected static Set<AlarmClientLeaf> getLeafItems(final AlarmTreeItem<?> root) {
		return streamLeafItems(root).collect(Collectors.toSet());
	}

	private static Stream<AlarmClientLeaf> streamLeafItems(final AlarmTreeItem<?> alarmTreeItem){
		if (alarmTreeItem instanceof AlarmClientLeaf alarmClientLeaf){
			return Stream.of(alarmClientLeaf);
		}
		else {
			return alarmTreeItem.getChildren().stream().flatMap(child -> streamLeafItems(child));
		}
	}

	/**
	 *
	 * @param items {@link List} of {@link AlarmTreeItem}s that may be a mix of leaves and non-leaves, e.g. a user
	 *                          selection in the alarm tree view.
	 * @return A {@link TreeNodeInfo} object.
	 */
	public static TreeNodeInfo getTreeNodeInfo(List<AlarmTreeItem<?>> items){
		Set<AlarmClientLeaf> leaves = getLeafItems(items);
		int disabledIndefinitely = 0;
		int disabledWithEnableDate = 0;
		Optional<LocalDateTime> localDateTime = Optional.empty();

		for(AlarmClientLeaf leaf : leaves){
			if(!leaf.isEnabled()){
				LocalDateTime enableDate = leaf.getEnabledDate();
				if(enableDate != null){
					if(localDateTime.isPresent() && !localDateTime.get().equals(enableDate)){
						localDateTime = Optional.empty();
					}
					else{
						localDateTime = Optional.of(enableDate);
					}
					disabledWithEnableDate++;
				}
				else{
					disabledIndefinitely++;
				}
			}
		}
		return new TreeNodeInfo(leaves, disabledIndefinitely, disabledWithEnableDate, localDateTime);
	}

	/**
	 *
	 * @param item A {@link AlarmTreeItem}, can be either a leaf or non-leaf in the alarm tree view.
	 * @return A {@link TreeNodeInfo} object.
	 */
	public static TreeNodeInfo getTreeNodeInfo(AlarmTreeItem<?> item){
		return getTreeNodeInfo(List.of(item));
	}

	/**
	 * Formats a {@link TreeNodeInfo} object based on its content.
	 * @param treeNodeInfo A {@link TreeNodeInfo}
	 * @return A string describing total number of leaves, disabled leaves (if any) and an enable date where applicable.
	 */
	public static String treeNodeInfoToString(TreeNodeInfo treeNodeInfo){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(MessageFormat.format(Messages.totalPVs, treeNodeInfo.leaves().size()));
		if(treeNodeInfo.disabled() > 0){
			stringBuilder.append(", ").append(MessageFormat.format(Messages.disabledIndefinitely, treeNodeInfo.disabled()));
		}
		int disabledWithEnableDate = treeNodeInfo.disabledWithEnableDate();
		if(disabledWithEnableDate > 0){
			stringBuilder.append(", ");
			if(treeNodeInfo.commonEnableDate().isPresent()){
				stringBuilder.append(MessageFormat.format(Messages.disabledCommonEnableDate, TimestampFormats.SECONDS_FORMAT.format(treeNodeInfo.commonEnableDate().get()), disabledWithEnableDate));
			}
			else{
				stringBuilder.append(MessageFormat.format(Messages.disabledVaryingEnableDate, disabledWithEnableDate));
			}
		}
		return stringBuilder.toString();
	}
}
