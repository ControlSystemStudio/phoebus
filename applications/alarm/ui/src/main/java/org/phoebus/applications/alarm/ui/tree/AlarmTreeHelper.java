package org.phoebus.applications.alarm.ui.tree;

import java.util.List;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeView;

public class AlarmTreeHelper {
	/**
	 * <p> Rebuild the tree structure by recreating the parents' children at the new path location.
	 * @param model AlarmClient used to communicate to the AlarmServer
	 * @param parent AlarmTreeItem whose children will be re-linked to the new path location.
	 * @param path Path that the parents' children will now be located at.
	 * @throws Exception
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

	/**
	 * <p> Prompt the screen with a dialog box. 
	 * @param title Title string of the dialog box.
	 * @param header Header string of the dialog box.
	 * @param node Node where the dialog box will be located.
	 * @param default_text Default text contents of dialog box.
	 * @return String The entered string or null if the entry was empty.
	 */
	public static String prompt(String title, String header,  final String default_text, final TreeView<AlarmTreeItem<?>> node)
    {
    	// Prompt for new name
        final TextInputDialog prompt = new TextInputDialog(default_text);
        DialogHelper.positionDialog(prompt, node, -200, -100);
        prompt.setTitle(title);
        prompt.setHeaderText(header);
        final String new_name = prompt.showAndWait().orElse(null);
        if (new_name == null || new_name.isEmpty())
            return null;
        return new_name;
    }

	/**
	 * <p> Validate the passed path string by checking if the path name is valid.
	 * <p> A path name is considered valid if all the elements making up the path are valid filenames,
	 * if the path to the items new location exists in the tree, and if the items new location is not
	 * a PV.
	 * <p> For example: If "top/middle/bottom/to_move" wanted to be moved to "top/middle/to_move". 
	 * The path "top/middle" must be in the tree view. And middle must not be a PV.
	 * <p> A path is considered valid if it contains only alphanumeric characters, '.' and '-'.
	 * <p> Each path element must match the regular expression [\\w.-]*
	 * @param path
	 * @return <code>true</code> if the pathname is valid, and if the path to the new location exists in the tree view.
	 */
	public static boolean validatePath(String path, TreeView<AlarmTreeItem<?>> tree_view)
	{
		String[] path_elems = AlarmTreePath.splitPath(path);
		// Make sure each element in the path is valid.
		for (String element : path_elems)
		{
		    if (! element.matches("[\\w.-]*"))
		        return false;
		}
		// Make sure the path exists
		// The proposed parent must exist.
		int elem_num = path_elems.length - 1;
		AlarmTreeItem<?> item = tree_view.getRoot().getValue();
		for (int i = 0; i < elem_num; i++)
		{
		    item = item.getChild(path_elems[i]);
		    if (null == item)
		        return false;
		    // Make sure the path does not contain a PV.
		    // PV cannot have children.
		    if (item instanceof AlarmClientLeaf)
		        return false;
		}
		return true;
	}
}
