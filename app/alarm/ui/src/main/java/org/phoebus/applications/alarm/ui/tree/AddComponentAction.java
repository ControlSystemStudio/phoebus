/*******************************************************************************
 * Copyright (c) 2018-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import java.util.List;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
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
        private final Label message = new Label();
        private final RadioButton type_node = new RadioButton("Node"),
                                  type_pv = new RadioButton("PV/s");

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
            type_pv.setTooltip(new Tooltip("Add a PV or a list of space-separated PVs to the alarm configuration"));

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

            layout.add(message, 1, 2);

            name.textProperty().addListener( (prop, old, value) -> checkName(value));

            setTitle("Add Component to " + parent.getPathName());
            getDialogPane().setContent(layout);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            setResizable(true);

            layout.setPrefWidth(600);

            setResultConverter(button ->
                button == ButtonType.OK ? name.getText() : null);

            // Initial focus on name
            Platform.runLater(() -> name.requestFocus());

            type_pv.selectedProperty().addListener(p ->
            {
                updateAutocompletion();
                // Selecting a type then also focuses on the name
                Platform.runLater(() -> name.requestFocus());
            });
            // Initial setting
            updateAutocompletion();
        }

        /** Add or remove PV name completion support based on type_pv */
        private void updateAutocompletion()
        {
            // Always OK to detach
            PVAutocompleteMenu.INSTANCE.detachField(name);
            // If PV names are required, attach
            if (type_pv.isSelected())
                PVAutocompleteMenu.INSTANCE.attachField(name);
        }

        private void checkName(final String name)
        {
            if (isPV())
            {
                final List<String> names = splitNames(name);
                if (names.size() <= 1)
                    message.setText("Enter one or more PV names, separated by spaces");
                else
                    message.setText("Adding " + names.size() + " PVs");
            }
            else
                message.setText("");
        }

        public boolean isPV()
        {
            return type_pv.isSelected();
        }

        // Allowing not just space as mentioned in tooltip and message, but also comma or semicolon
        public static List<String> splitNames(final String names)
        {
            return List.of(names.split("[\\s,;]+"));
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

            // Add in background thread
            JobManager.schedule(getText(), monitor ->
            {
                final AlarmTreeItem<?> root = getRoot(parent);
                if (dialog.isPV())
                {
                    final List<String> new_names = AddComponentDialog.splitNames(new_name);
                    for (String pv : new_names)
                    {
                        // Check for item of same name at that level
                        if (haveExistingItem(node, parent, pv))
                            break;
                        // Check for duplicate PV, anywhere in alarm tree
                        final String existing = findPV(root, pv);
                        if (existing == null)
                            model.addPV(parent.getPathName(), pv);
                        else
                        {
                            Platform.runLater(() ->
                            {
                                final Alert error = new Alert(AlertType.ERROR);
                                error.setTitle(getText());
                                error.setHeaderText("Cannot add PV " + pv);
                                error.setContentText("Duplicate for " + existing);
                                error.setResizable(true);
                                DialogHelper.positionDialog(error, node, -100, -50);
                                error.showAndWait();
                            });
                            break;
                        }
                    }
                }
                else
                    if (! haveExistingItem(node, parent, new_name))
                        model.addComponent(parent.getPathName(), new_name);
            });
        });
    }

    /** @param node Node to position dialog
     *  @param parent Parent in alarm tree
     *  @param new_name New name to add to parent
     *  @return <code>true</code> if parent already has item of that name,
     *          and alert dialog has been scheduled.
     */
    private boolean haveExistingItem(final Node node, final AlarmTreeItem<?> parent, final String new_name)
    {
        // Check for name name on that level
        if (parent.getChild(new_name) == null)
            return false;
        Platform.runLater(() ->
        {
            final Alert error = new Alert(AlertType.ERROR);
            error.setTitle(getText());
            error.setHeaderText("Cannot add " + new_name);
            error.setContentText("Duplicate for existing item of same name at " + parent.getPathName());
            error.setResizable(true);
            DialogHelper.positionDialog(error, node, -100, -50);
            error.showAndWait();
        });
        return true;
    }

    /** @param item {@link AlarmTreeItem}
     *  @return Root element of alarm tree
     */
    private AlarmTreeItem<?> getRoot(AlarmTreeItem<?> item)
    {
        AlarmTreeItem<?> root = item;
        while (root.getParent() != null)
            root = root.getParent();
        return root;
    }

    /** @param item Alarm tree item (initially root) where to search for PV
     *  @param pv Name of PV
     *  @return PV or <code>null</code> when not found
     */
    private String findPV(final AlarmTreeItem<?> item, final String pv)
    {
        if (item instanceof  AlarmTreeLeaf)
        {
            if (item.getName().equals(pv))
                return item.getPathName();
        }
        else
            for (AlarmTreeItem<?> sub : item.getChildren())
            {
                final String found = findPV(sub, pv);
                if (found != null)
                    return found;
            }
        return null;
    }
}
