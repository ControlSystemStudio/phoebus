package org.phoebus.applications.alarm;

import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.tree.AlarmTreeHelper;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class AlarmTreeValidatePathTest
{
    public void testValidatePath()
    {
        AlarmClientNode root = new AlarmClientNode(null, "root");
        AlarmClientNode area1 = new AlarmClientNode(root, "Area 1");
        AlarmClientNode area2 = new AlarmClientNode(root, "Area 2");
        AlarmClientNode area3 = new AlarmClientNode(area1, "Area 3");
        AlarmClientNode area4 = new AlarmClientNode(area2, "Area 4");
        TreeItem<AlarmTreeItem<?>> tree_item = new TreeItem<AlarmTreeItem<?>>(root);
        TreeView<AlarmTreeItem<?>> tree_view = new TreeView<AlarmTreeItem<?>>(tree_item);
        
        AlarmTreeHelper.validatePath("", tree_view);
    }
}
