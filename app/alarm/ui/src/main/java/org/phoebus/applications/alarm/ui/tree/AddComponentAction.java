/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Action that adds a new item to the alarm tree configuration
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class AddComponentAction extends MenuItem
{
    private static class AddComponentDialog extends Dialog<String>
    {
        private final TextField name = new TextField();
        private final RadioButton type_node = new RadioButton("Node"),
                                  type_pv = new RadioButton("PV");

        public AddComponentDialog(final AlarmTreeItem<?> parent)
        {
            final GridPane layout = new GridPane();
            // layout.setGridLinesVisible(true);
            layout.setHgap(5);
            layout.setVgap(5);

            layout.add(new Label("Type:"), 0, 0);

            final ToggleGroup types = new ToggleGroup();
            type_node.setToggleGroup(types);
            type_node.setTooltip(new Tooltip("Create a new node in the alarm configuration hierachy"));

            type_pv.setToggleGroup(types);
            type_pv.setTooltip(new Tooltip("Add a PV to the alarm configuration"));

            layout.add(new HBox(5, type_node, type_pv), 1, 0);

            // For 'main' and the 'subsystem' level suggest adding another node.
            // Further below, suggest PV
            if (parent.getParent() == null ||
                parent.getParent().getParent() == null)
                type_node.setSelected(true);
            else
                type_pv.setSelected(true);

            layout.add(new Label("Name:"), 0, 1);
            name.setTooltip(new Tooltip("Name of new node or PV"));
            GridPane.setHgrow(name, Priority.ALWAYS);
            layout.add(name, 1, 1);

            setTitle("Add Component to " + parent.getPathName());
            getDialogPane().setContent(layout);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            setResizable(true);

            layout.setPrefWidth(600);

            setResultConverter(button ->
                button == ButtonType.OK ? name.getText() : null);

            // Initial focus on name
            Platform.runLater(() -> name.requestFocus());

            // Selecting a type then also focuses on the name
            type_pv.selectedProperty().addListener(p -> Platform.runLater(() -> name.requestFocus()));
        }

        public boolean isPV()
        {
            return type_pv.isSelected();
        }
    }

    /** @param node Node to position dialog
     *  @param model Model where new component is added
     *  @param parent Parent item in alarm tree
     */
    public AddComponentAction(final Node node, final AlarmClient model, final AlarmTreeItem<?> parent)
    {
        super("Add Component", ImageCache.getImageView(ImageCache.class, "/icons/add.png"));
        setOnAction(event ->
        {
            final AddComponentDialog dialog = new AddComponentDialog(parent);
            DialogHelper.positionDialog(dialog, node, -100, -50);
            final String new_name = dialog.showAndWait().orElse(null);
            if (new_name == null  ||  new_name.isEmpty())
                return;
            
            JobManager.schedule(getText(), monitor ->
            {
                if (dialog.isPV())
                    model.addPV(parent.getPathName(), new_name);
                else
                    model.addComponent(parent.getPathName(), new_name);
            });
        });
    }
}
