/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/** Dialog to select items from a list of available items and build a list of selected items.
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class ListSelectionDialog extends Dialog<Boolean>
{
    private static final int buttonWidth = 110, spacing = 10;
    private static final Font labelFont = new Font(16);

    private final Function<String, Boolean> addSelected, removeSelected;

    private final ListView<String> availableItems, selectedItems;

    public ListSelectionDialog(final Node root,
                               final String title,
                               final Supplier<ObservableList<String>>    available,
                               final Supplier<ObservableList<String>>    selected,
                               final Function<String, Boolean> addSelected,
                               final Function<String, Boolean> removeSelected)
    {
        this.addSelected    = addSelected;
        this.removeSelected = removeSelected;

        selectedItems  = new ListView<>(selected.get());
        // We want to remove items from the available list as they're selected, and add them back as they are unselected.
        // Due to this we need a copy as available.get() returns an immutable list.
        availableItems = new ListView<>(
                FXCollections.observableArrayList(new ArrayList<>(available.get())));

        // Remove what's already selected from the available items
        for (String item : selectedItems.getItems())
            availableItems.getItems().remove(item);

        setTitle(title);

        final ButtonType apply = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);

        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, apply);
        getDialogPane().setContent(formatContent());

        setResizable(true);

        DialogHelper.positionAndSize(this, root,
                PhoebusPreferenceService.userNodeForClass(ListSelectionDialog.class),
                500, 600);

        setResultConverter(button ->  button == apply);
    }

    private HBox formatContent()
    {
        selectedItems.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        availableItems.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedItems.setStyle("-fx-control-inner-background-alt: white");
        availableItems.setStyle("-fx-control-inner-background-alt: white");

        final Button add = new Button("Add", ImageCache.getImageView(ImageCache.class, "/icons/add.png"));
        add.setTooltip(new Tooltip("Add the selected items."));
        add.setOnAction(event -> addSelectedItems());

        final Button remove = new Button("Remove", ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
        remove.setTooltip(new Tooltip("Remove the selected items."));
        remove.setOnAction(event -> removeItems(selectedItems.getSelectionModel().getSelectedItems()));

        final Button clear  = new Button("Clear", ImageCache.getImageView(ImageCache.class, "/icons/remove_multiple.png"));
        clear.setTooltip(new Tooltip("Clear the selected items list."));
        clear.setOnAction(event ->  removeItems(selectedItems.getItems()));

        add.setPrefWidth(buttonWidth);
        remove.setPrefWidth(buttonWidth);
        clear.setPrefWidth(buttonWidth);

        add.setMinWidth(buttonWidth);
        remove.setMinWidth(buttonWidth);
        clear.setMinWidth(buttonWidth);

        // Enable buttons as appropriate
        add.disableProperty().bind(Bindings.isEmpty(availableItems.getSelectionModel().getSelectedItems()));
        remove.disableProperty().bind(Bindings.isEmpty(selectedItems.getSelectionModel().getSelectedItems()));
        clear.disableProperty().bind(Bindings.isEmpty(selectedItems.getItems()));

        // Double click to add..
        availableItems.setOnMouseClicked(event ->
        {
            if (event.getClickCount() < 2)
                return;
            addSelectedItems();
            event.consume();
        });
        // .. or remove items
        selectedItems.setOnMouseClicked(event ->
        {
            if (event.getClickCount() < 2)
                return;
            removeItems(selectedItems.getSelectionModel().getSelectedItems());
            event.consume();
        });

        final VBox buttonsBox = new VBox(10, add, remove, clear);
        buttonsBox.setSpacing(10);
        buttonsBox.setAlignment(Pos.CENTER);

        final Label itemsLabel = new Label("Available");
        itemsLabel.setFont(labelFont);
        VBox.setVgrow(availableItems, Priority.ALWAYS);
        final VBox availableBox = new VBox(spacing, itemsLabel, availableItems);

        final Label selectedLabel = new Label("Selected");
        selectedLabel.setFont(labelFont);
        VBox.setVgrow(selectedItems, Priority.ALWAYS);
        final VBox selectedBox = new VBox(spacing, selectedLabel, selectedItems);

        HBox.setHgrow(availableBox, Priority.ALWAYS);
        HBox.setHgrow(selectedBox, Priority.ALWAYS);

        HBox.setMargin(availableBox, new Insets(5,  0, 10, 10));
        HBox.setMargin(buttonsBox,   new Insets(5,  0, 10,  0));
        HBox.setMargin(selectedBox,  new Insets(5, 10, 10,  0));

        final HBox content = new HBox(spacing, availableBox, buttonsBox, selectedBox);
        content.setAlignment(Pos.CENTER);
        return content;
    }

    private void addSelectedItems()
    {
        // Can't modify list we're iterating over, so make a copy to iterate over.
        for (String item : new ArrayList<>(availableItems.getSelectionModel().getSelectedItems()))
        {
            addSelected.apply(item);
            availableItems.getItems().remove(item);
        }
        clearSelections();
    }

    private void removeItems(final List<String> items)
    {
        for (String item : new ArrayList<>(items))
        {
            removeSelected.apply(item);
            availableItems.getItems().add(item);
        }
        clearSelections();
        Collections.sort(availableItems.getItems());
    }

    private void clearSelections()
    {
        selectedItems.getSelectionModel().clearSelection();
        availableItems.getSelectionModel().clearSelection();
    }
}