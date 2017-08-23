/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtree;

import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockStage;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/** PV Tree Application
 *  @author Kay Kasemir
 */
// @ProviderFor(Application.class)
public class PVTreeApplication // implements Application
{
    public static final String NAME = "PV Tree";

    private final TextField pv_name = new TextField();
    private final TreeView<String> tree = new TreeView<>();

    public String getName()
    {
        return NAME;
    }

    public void start(final Stage stage)
    {
        final Label label = new Label("PV Name:");
        pv_name.setOnAction(event -> setPVName(pv_name.getText()));

        final ToggleButton latch = new ToggleButton(null, getImageView("run.png"));
        latch.setTooltip(new Tooltip("Stop updates on first alarm?"));
        latch.setOnAction(event ->
        {
//            model.latchOnAlarm(latch.isSelected());
            if (latch.isSelected())
                latch.setGraphic(getImageView("pause_on_alarm.png"));
            else
                latch.setGraphic(getImageView("run.png"));
        });

        final Button collapse = new Button(null, getImageView("collapse.gif"));
        collapse.setTooltip(new Tooltip("Collapse all tree items"));
//        collapse.setOnAction(event -> tree.expandAll(false));

        final Button alarms = new Button(null, getImageView("alarmtree.png"));
        alarms.setTooltip(new Tooltip("Show all tree items that are in alarm"));
//        alarms.setOnAction(event -> tree.expandAlarms());

        final Button expand = new Button(null, getImageView("pvtree.png"));
        expand.setTooltip(new Tooltip("Show complete tree"));
//        expand.setOnAction(event -> tree.expandAll(true));

        final TreeItem<String> root = new TreeItem<>("A");
        root.getChildren().add(new TreeItem<String>("INPA"));
        root.getChildren().add(new TreeItem<String>("INPB"));
        tree.setRoot(root);

        // center vertically
        label.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(pv_name, Priority.ALWAYS);
        final HBox top = new HBox(5, label, pv_name, latch, collapse, alarms, expand);
        BorderPane.setMargin(top, new Insets(5, 5, 0, 5));
        BorderPane.setMargin(tree, new Insets(5));
        final BorderPane layout = new BorderPane(tree);
        layout.setTop(top);

        final DockItem tab = new DockItem(getName(), layout);
        DockStage.getDockPane(stage).addTab(tab);
    }

    public void stop()
    {
    }

    private ImageView getImageView(final String icon)
    {
        return new ImageView(new Image(getClass().getResourceAsStream("/icons/" + icon)));
    }

    private void setPVName(String name)
    {
        name = name.trim();
        pv_name.setText(name);

        // TODO Auto-generated method stub
    }
}
