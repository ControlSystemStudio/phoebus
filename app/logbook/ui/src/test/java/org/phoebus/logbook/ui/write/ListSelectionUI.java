/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.phoebus.ui.javafx.ClearingTextField;
import org.testfx.framework.junit.ApplicationTest;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Test the functionality of the ListSelectionDialog
 * @author Evan Smith
 */
public class ListSelectionUI extends ApplicationTest
{
    /** Skip test when java.awt.headless=true */
    private boolean skip = Boolean.parseBoolean(System.getProperty("java.awt.headless"));

    public class MainPane extends BorderPane
    {
        Stage stage;
        ObservableList<String> available;
        ObservableList<String> selected;

        public MainPane(Stage stage)
        {
            this.stage = stage;
            available = FXCollections.observableArrayList();
            selected  = FXCollections.observableArrayList();

            for (int i = 0; i < 50; i++)
                available.add("item " + i);

            stage.setOnShown(event -> Platform.runLater(this::showDialog));
        }

        private void showDialog()
        {
            ListSelectionDialog dialog = new ListSelectionDialog(stage.getScene().getRoot(), "TEST", this::available, this::selected, this::addSelected, this::removeSelected);
            dialog.showAndWait();
        }

        public ObservableList<String> available()
        {
            return available;
        }

        public ObservableList<String> selected()
        {
            return selected;
        }

        public Boolean addSelected(String item)
        {
            return selected.add(item);
        }

        public Boolean removeSelected(String item)
        {
            return selected.remove(item);
        }
    }

    @Override
    public void start(Stage stage) throws TimeoutException {
        if (skip)
            return;
        stage.setScene(new Scene(new MainPane(stage)));
        stage.show();
    }

    public void myClickOn(Button button) throws Exception
    {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Platform.runLater(() ->
        {
            button.fire();
            future.complete(null);
        });

        future.get();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAndRemoveAllItems() throws Exception
    {
        if (skip)
            return;
        ListView<String> availableItems = (ListView<String>) lookup("#availableItems").query();
        ListView<String> selectedItems = (ListView<String>) lookup("#selectedItems").query();

        Button add = (Button) lookup("#addButton").query();
        Button remove = (Button) lookup("#removeButton").query();

        // Check that there are no selected items.
        assertTrue(selectedItems.getItems().isEmpty());

        // Check that add is disabled.
        assertTrue(add.disabledProperty().get());
        // Select all the available items.
        availableItems.getSelectionModel().selectAll();
        // Check that add is enabled.
        assertFalse(add.disabledProperty().get());

        myClickOn(add);

        // Check that selected items is not empty.
        assertFalse(selectedItems.getItems().isEmpty());
        // Check that available items is not empty.
        assertTrue(availableItems.getItems().isEmpty());

        // Check that remove is disabled.
        assertTrue(remove.disabledProperty().get());
        // Select all the selected items.
        selectedItems.getSelectionModel().selectAll();
        // Check that remove is enabled.
        assertFalse(remove.disabledProperty().get());

        myClickOn(remove);

        // Check that selected items is empty.
        assertTrue(selectedItems.getItems().isEmpty());
        // With no selected items, check that remove is disabled.
        assertTrue(remove.disabledProperty().get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAndClearAllItems() throws Exception
    {
        if (skip)
            return;
        ListView<String> availableItems = (ListView<String>) lookup("#availableItems").query();
        ListView<String> selectedItems = (ListView<String>) lookup("#selectedItems").query();

        Button add = (Button) lookup("#addButton").query();
        Button clear = (Button) lookup("#clearButton").query();

        // Check that no items are selected.
        assertTrue(selectedItems.getItems().isEmpty());

        // Check that add is disabled.
        assertTrue(add.disabledProperty().get());

        // Select all the items.
        availableItems.getSelectionModel().selectAll();

        // Check that add is now enabled.
        assertFalse(add.disabledProperty().get());

        myClickOn(add);

        // Check that the items were indeed added.
        assertFalse(selectedItems.getItems().isEmpty());
        assertTrue(availableItems.getItems().isEmpty());

        // Check that clear is not disabled.
        assertFalse(clear.disabledProperty().get());

        myClickOn(clear);

        // Check that there are no selected items.
        assertTrue(selectedItems.getItems().isEmpty());
        // With no selected items, check that clear is disabled.
        assertTrue(clear.disabledProperty().get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAndRemoveItems() throws Exception
    {
        if (skip)
            return;
        ListView<String> availableItems = (ListView<String>) lookup("#availableItems").query();
        ListView<String> selectedItems = (ListView<String>) lookup("#selectedItems").query();

        Button add = (Button) lookup("#addButton").query();
        Button remove = (Button) lookup("#removeButton").query();

        String[] items = {"item 1", "item 3", "item 5", "item 7"};

        // Check that no items are selected.
        assertTrue(selectedItems.getItems().isEmpty());

        // Check that add is disabled.
        assertTrue(add.disabledProperty().get());

        // Select 4 items.
        for (String item : items)
            availableItems.getSelectionModel().select(item);

        // Check that add is now enabled.
        assertFalse(add.disabledProperty().get());

        myClickOn(add);

        // Check that the items were added.
        for (String item : items)
            assertFalse(availableItems.getItems().contains(item));
        for (String item : items)
            assertTrue(selectedItems.getItems().contains(item));

        // Check that remove is disabled.
        assertTrue(remove.disabledProperty().get());

        // Select the selected items.
        for (String item : items)
            selectedItems.getSelectionModel().select(item);

        // Check that remove is enabled.
        assertFalse(remove.disabledProperty().get());

        myClickOn(remove);

        // Check that the items were removed.
        for (String item : items)
            assertFalse(selectedItems.getItems().contains(item));

        // Check that the items were returned.
        for (String item : items)
            assertTrue(availableItems.getItems().contains(item));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAndClearItems() throws Exception
    {
        if (skip)
            return;
        ListView<String> availableItems = (ListView<String>) lookup("#availableItems").query();
        ListView<String> selectedItems = (ListView<String>) lookup("#selectedItems").query();

        Button add = (Button) lookup("#addButton").query();
        Button clear = (Button) lookup("#clearButton").query();

        String[] items = {"item 2", "item 4", "item 6", "item 8"};

        // Check that no items are selected.
        assertTrue(selectedItems.getItems().isEmpty());

        // Check that add is disabled.
        assertTrue(add.disabledProperty().get());

        // Select 4 items.
        for (String item : items)
            availableItems.getSelectionModel().select(item);

        // Check that add is now enabled.
        assertFalse(add.disabledProperty().get());

        myClickOn(add);

        // Check that the items were added.
        for (String item : items)
            assertFalse(availableItems.getItems().contains(item));
        for (String item : items)
            assertTrue(selectedItems.getItems().contains(item));

        // Check that clear is not disabled.
        assertFalse(clear.disabledProperty().get());

        myClickOn(clear);

        // Check that no items are selected.
        assertTrue(selectedItems.getItems().isEmpty());

        // Check that the items were removed.
        for (String item : items)
            assertFalse(selectedItems.getItems().contains(item));

        // Check that the items were returned.
        for (String item : items)
            assertTrue(availableItems.getItems().contains(item));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchBar()
    {
        if (skip)
            return;
        ListView<String> availableItems = (ListView<String>) lookup("#availableItems").query();
        ClearingTextField searchField = (ClearingTextField) lookup("#searchField").query();

        String[] items = {"item 1", "item 10", "item 11", "item 12", "item 13", "item 14", "item 15", "item 16", "item 17", "item 18", "item 19"};

        // Check that the search bar is empty.
        assertTrue(searchField.getText().isEmpty());

        // Type something in the search bar that selects all the items.
        searchField.setText("item");

        // Check that the available items containing the typed substring are selected.
        assertEquals(availableItems.getItems().size(), availableItems.getSelectionModel().getSelectedItems().size());

        // Clear the search bar.
        searchField.setText("");

        // Check that no available items are selected.
        assertTrue(availableItems.getSelectionModel().getSelectedItems().isEmpty());

        // Type something in the search bar that selects a subset of the items.
        searchField.setText("item 1");

        // Check that the available items containing the typed substring are selected.
        assertEquals(11, availableItems.getSelectionModel().getSelectedItems().size());
        for (String item : items)
            assertTrue(availableItems.getSelectionModel().getSelectedItems().contains(item));

        // Clear the search bar.
        searchField.setText("");

        // Check that no available items are selected.
        assertTrue(availableItems.getSelectionModel().getSelectedItems().isEmpty());
    }
}
