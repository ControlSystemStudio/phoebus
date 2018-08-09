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
import org.phoebus.ui.javafx.ClearingTextField;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
    
    /* Translations would apply to these strings. Can be moved to NLS */
    private static final String ADD = "Add",                                
                                ADD_TOOLTIP = "Add the selected items.",
                                APPLY = "Apply",
                                AVAILABLE = "Available", 
                                CLEAR = "Clear",                                
                                CLEAR_TOOLTIP = "Clear the selected items list.",
                                NUM_SELECTED = "selected",
                                REMOVE = "Remove",
                                REMOVE_TOOLTIP = "Remove the selected items.",
                                SEARCH = "Search available:",
                                SELECTED = "Selected";
    
    /* Non NLS Strings */
    private static final String ADD_ID = "addButton",
                                ADD_ICON = "/icons/add.png",
                                AVAILABLE_ID = "availableItems",
                                CLEAR_ICON = "/icons/remove_multiple.png",
                                CLEAR_ID = "clearButton",
                                REMOVE_ICON = "/icons/delete.png",
                                REMOVE_ID = "removeButton",
                                SEARCH_ID = "searchField",
                                SELECTED_ID = "selectedItems";
    
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
        
        final ButtonType apply = new ButtonType(APPLY, ButtonBar.ButtonData.OK_DONE);

        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, apply);
        getDialogPane().setContent(formatContent());

        setResizable(true);

        DialogHelper.positionAndSize(this, root,
                PhoebusPreferenceService.userNodeForClass(ListSelectionDialog.class),
                500, 600);

        setResultConverter(button ->  button == apply);
    }

    private VBox formatContent()
    {
        selectedItems.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        availableItems.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedItems.setStyle("-fx-control-inner-background-alt: white");
        availableItems.setStyle("-fx-control-inner-background-alt: white");
        selectedItems.setId(SELECTED_ID);
        availableItems.setId(AVAILABLE_ID);
        
        final Button add = new Button(ADD, ImageCache.getImageView(ImageCache.class, ADD_ICON));
        add.setTooltip(new Tooltip(ADD_TOOLTIP));
        add.setOnAction(event -> addSelectedItems());

        final Button remove = new Button(REMOVE, ImageCache.getImageView(ImageCache.class, REMOVE_ICON));
        remove.setTooltip(new Tooltip(REMOVE_TOOLTIP));
        remove.setOnAction(event -> removeItems(selectedItems.getSelectionModel().getSelectedItems()));

        final Button clear  = new Button(CLEAR, ImageCache.getImageView(ImageCache.class, CLEAR_ICON));
        clear.setTooltip(new Tooltip(CLEAR_TOOLTIP));
        clear.setOnAction(event ->  removeItems(selectedItems.getItems()));

        add.setPrefWidth(buttonWidth);
        remove.setPrefWidth(buttonWidth);
        clear.setPrefWidth(buttonWidth);

        add.setMinWidth(buttonWidth);
        remove.setMinWidth(buttonWidth);
        clear.setMinWidth(buttonWidth);
        
        add.setId(ADD_ID);
        remove.setId(REMOVE_ID);
        clear.setId(CLEAR_ID);
        
        // Note: For the followings, trying to initialize right away resulted in buttons that remained
        // disabled or would not re-enable.
        // Only runLater(..) seems to fully function...
        // Enable buttons as appropriate
        Platform.runLater( () -> 
        {
           add.disableProperty().bind(Bindings.isEmpty(availableItems.getSelectionModel().getSelectedItems()));
           remove.disableProperty().bind(Bindings.isEmpty(selectedItems.getSelectionModel().getSelectedItems()));
           clear.disableProperty().bind(Bindings.isEmpty(selectedItems.getItems()));
        });
        
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

        final Label availableLabel = new Label(AVAILABLE);
        availableLabel.setFont(labelFont);
        VBox.setVgrow(availableItems, Priority.ALWAYS);
        final VBox availableBox = new VBox(spacing, availableLabel, availableItems);

        availableItems.getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super String>) c -> 
        {
            int selectedNum = availableItems.getSelectionModel().getSelectedItems().size();
            if (selectedNum > 0)
                Platform.runLater(() -> availableLabel.setText(AVAILABLE + " (" + selectedNum + " " + NUM_SELECTED + ")"));
            else
                Platform.runLater(() -> availableLabel.setText(AVAILABLE));
        });
        
        final Label selectedLabel = new Label(SELECTED);
        selectedLabel.setFont(labelFont);
        VBox.setVgrow(selectedItems, Priority.ALWAYS);
        final VBox selectedBox = new VBox(spacing, selectedLabel, selectedItems);

        selectedItems.getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super String>) c -> 
        {
            int selectedNum = selectedItems.getSelectionModel().getSelectedItems().size();
            if (selectedNum > 0)
                Platform.runLater(() -> selectedLabel.setText(SELECTED + " (" + selectedNum + " " + NUM_SELECTED + ")"));
            else
                Platform.runLater(() -> selectedLabel.setText(SELECTED));
        });

        HBox.setHgrow(availableBox, Priority.ALWAYS);
        HBox.setHgrow(selectedBox, Priority.ALWAYS);

        HBox.setMargin(availableBox, new Insets(5,  0, 10, 10));
        HBox.setMargin(buttonsBox,   new Insets(5,  0, 10,  0));
        HBox.setMargin(selectedBox,  new Insets(5, 10, 10,  0));
        
        final HBox selectionBox = new HBox(spacing, availableBox, buttonsBox, selectedBox);
        selectionBox.setAlignment(Pos.CENTER);
        
        final ClearingTextField searchField = new ClearingTextField();
        searchField.setId(SEARCH_ID);
        searchField.textProperty().addListener((changeListener, oldVal, newVal) -> 
        {
            searchAvailableItemsForSubstring(newVal);
        });
        
        final Label searchLabel = new Label(SEARCH);
        searchLabel.setFont(labelFont);
        final HBox searchBox = new HBox(spacing, searchLabel, searchField);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        final VBox content = new VBox(spacing, searchBox, selectionBox);
        VBox.setVgrow(selectionBox, Priority.ALWAYS);
        VBox.setMargin(searchBox, new Insets(10, 10, 0, 10));
        
        return content;
    }

    private void searchAvailableItemsForSubstring(final String substring)
    {
        if (substring.trim().isEmpty())
            availableItems.getSelectionModel().clearSelection();
        else
        {                
            int size = availableItems.getItems().size();
            for (int i = 0; i < size; i++)
            {
                final String item = availableItems.getItems().get(i);
                if (item.contains(substring))
                    availableItems.getSelectionModel().select(i);
                else
                    availableItems.getSelectionModel().clearSelection(i);
            }
        }
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
