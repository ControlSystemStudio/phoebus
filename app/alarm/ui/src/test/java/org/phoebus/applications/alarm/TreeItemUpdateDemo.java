/*******************************************************************************
 * Copyright (c) 2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Demo of alarm tree cell update
 *
 *  Demonstrates refresh issue for tree cells that are
 *  not visible because they scrolled below the window bottom.
 *
 *  Only replacing the TreeItem results in reliable tree updates.
 *
 *  1) Start this program, click the "On" and "Off" buttons, and observe how item "Two" is updated
 *  2) Click "Off", reduce the window height so that the items under "Top" are hidden,
 *     click "On", increase window height to again show all items.
 *     Would expect to see "On", but tree still shows "Off"
 *     until the tree is collapsed and again expanded.
 *  3) Click "On", reduce the window height so that the items under "Top" are hidden,
 *     click "Off", increase window height to again show all items.
 *     Tree should indeed show "Off".
 *
 *  Conclusion is that TreeItem.setValue() does not trigger updates of certain hidden items.
 *  Basic google search suggests that the value must change, but "On" and "Off"
 *  are different object references and they are not 'equal'.
 *
 *  When instead replacing the complete TreeItem, the tree is always updated.
 *
 *  Tested with JavaFX 15, 19, 20
 *
 *  @author Kay Kasemir
 */
public class TreeItemUpdateDemo extends ApplicationWrapper
{
    private TreeView<String> tree_view = new TreeView<>();

    @Override
    public void start(final Stage stage)
    {
        final Button on = new Button("On");
        on.setOnAction(event ->
        {   // Just updating the value of TreeItem is ignored for hidden items?
            tree_view.getRoot().getChildren().get(1).setValue("On");
        });
        final Button off = new Button("Off");
        off.setOnAction(event ->
        {   // Replacing the TreeItem always "works"?
            // (Note this would be more work for intermediate items
            //  that have child nodes, since child nodes need to be moved/copied)
            tree_view.getRoot().getChildren().set(1, new TreeItem<>("Off"));
        });
        final HBox bottom = new HBox(on, off);
        on.setMaxWidth(1000);
        off.setMaxWidth(1000);
        HBox.setHgrow(on, Priority.ALWAYS);
        HBox.setHgrow(off, Priority.ALWAYS);

        final VBox root = new VBox(tree_view, bottom);
        VBox.setVgrow(tree_view, Priority.ALWAYS);

        TreeItem<String> top = new TreeItem<>("Top");
        tree_view.setRoot(top);

        top.getChildren().add(new TreeItem<>("One"));
        top.getChildren().add(new TreeItem<>("Two"));
        top.getChildren().add(new TreeItem<>("Three"));
        top.setExpanded(true);

        final Scene scene = new Scene(root, 300, 300);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(TreeItemUpdateDemo.class, args);
    }
}
