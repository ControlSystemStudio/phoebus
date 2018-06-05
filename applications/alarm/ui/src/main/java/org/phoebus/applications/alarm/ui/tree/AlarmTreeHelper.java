package org.phoebus.applications.alarm.ui.tree;

import java.util.List;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeView;

public class AlarmTreeHelper {
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

	public static String prompt(String title, String header, final TreeView<AlarmTreeItem<?>> node, final AlarmTreeItem<?> item)
    {
    	// Prompt for new name
        final TextInputDialog prompt = new TextInputDialog(item.getName());
        DialogHelper.positionDialog(prompt, node, -200, -100);
        prompt.setTitle(title);
        prompt.setHeaderText(header);
        final String new_name = prompt.showAndWait().orElse(null);
        if (new_name == null || new_name.isEmpty())
            return null;
        return new_name;
    }
}
