/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeoutException;

import org.junit.Test;
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
public class TestListSelectionDialog extends ApplicationTest
{
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
        stage.setScene(new Scene(new MainPane(stage)));
        stage.show();
    }

    @SuppressWarnings("unchecked")
    @Test 
    public void testAddAndRemoveAllItems()
    {
        ListView<String> availableItems = (ListView<String>) lookup("#availableItems").query();
        ListView<String> selectedItems = (ListView<String>) lookup("#selectedItems").query();
        
        Button add = (Button) lookup("#addButton").query();
        Button remove = (Button) lookup("#removeButton").query();
        
        // Check that there are no selected items.
        assertEquals(true, selectedItems.getItems().isEmpty());

        // Check that add is disabled.
        assertEquals(true, add.disabledProperty().get());
        // Select all the available items.
        availableItems.getSelectionModel().selectAll();
        // Check that add is enabled.
        assertEquals(false, add.disabledProperty().get());
        
        clickOn("#addButton");
        
        // Check that selected items is not empty.
        assertEquals(false, selectedItems.getItems().isEmpty());
        // Check that available items is not empty.
        assertEquals(true, availableItems.getItems().isEmpty());

        // Check that remove is disabled.
        assertEquals(true, remove.disabledProperty().get());
        // Select all the selected items.
        selectedItems.getSelectionModel().selectAll();
        // Check that remove is enabled.
        assertEquals(false, remove.disabledProperty().get());
        
        clickOn("#removeButton");
        
        // Check that selected items is empty.
        assertEquals(true, selectedItems.getItems().isEmpty());
        // With no selected items, check that remove is disabled.
        assertEquals(true, remove.disabledProperty().get());
    }
    
    @SuppressWarnings("unchecked")
    @Test 
    public void testAddAndClearAllItems()
    {
        ListView<String> availableItems = (ListView<String>) lookup("#availableItems").query();
        ListView<String> selectedItems = (ListView<String>) lookup("#selectedItems").query();
        
        Button add = (Button) lookup("#addButton").query();
        Button clear = (Button) lookup("#clearButton").query();
        
        // Check that no items are selected.
        assertEquals(true, selectedItems.getItems().isEmpty());
        
        // Check that add is disabled.
        assertEquals(true, add.disabledProperty().get());
        
        // Select all the items.
        availableItems.getSelectionModel().selectAll();
        
        // Check that add is now enabled.
        assertEquals(false, add.disabledProperty().get());
        
        clickOn("#addButton");
        
        // Check that the items were indeed added.
        assertEquals(false, selectedItems.getItems().isEmpty());
        assertEquals(true, availableItems.getItems().isEmpty());
        
        // Check that clear is not disabled.
        assertEquals(false, clear.disabledProperty().get());
        
        clickOn("#clearButton");
        
        // Check that there are no selected items.
        assertEquals(true, selectedItems.getItems().isEmpty());
        // With no selected items, check that clear is disabled.
        assertEquals(true, clear.disabledProperty().get());
    }
    
    @SuppressWarnings("unchecked")
    @Test 
    public void testAddAndRemoveItems()
    {
        ListView<String> availableItems = (ListView<String>) lookup("#availableItems").query();
        ListView<String> selectedItems = (ListView<String>) lookup("#selectedItems").query();
        
        Button add = (Button) lookup("#addButton").query();
        Button remove = (Button) lookup("#removeButton").query();
        
        String[] items = {"item 1", "item 3", "item 5", "item 7"};
        
        // Check that no items are selected.
        assertEquals(true, selectedItems.getItems().isEmpty());
        
        // Check that add is disabled.
        assertEquals(true, add.disabledProperty().get());
        
        // Select 4 items.
        for (String item : items)
            availableItems.getSelectionModel().select(item);
        
        // Check that add is now enabled.
        assertEquals(false, add.disabledProperty().get());
        
        clickOn("#addButton");
        
        // Check that the items were added.
        for (String item : items)
            assertEquals(false, availableItems.getItems().contains(item));
        for (String item : items)
            assertEquals(true, selectedItems.getItems().contains(item));

        // Check that remove is disabled.
        assertEquals(true, remove.disabledProperty().get());
        
        // Select the selected items.
        for (String item : items)
            selectedItems.getSelectionModel().select(item);
        
        // Check that remove is enabled.
        assertEquals(false, remove.disabledProperty().get());
        
        clickOn("#removeButton");
        
        // Check that the items were removed.
        for (String item : items)
            assertEquals(false, selectedItems.getItems().contains(item));
        
        // Check that the items were returned.
        for (String item : items)
            assertEquals(true, availableItems.getItems().contains(item));
    }
    
    @SuppressWarnings("unchecked")
    @Test 
    public void testAddAndClearItems()
    {
        ListView<String> availableItems = (ListView<String>) lookup("#availableItems").query();
        ListView<String> selectedItems = (ListView<String>) lookup("#selectedItems").query();
        
        Button add = (Button) lookup("#addButton").query();
        Button clear = (Button) lookup("#clearButton").query();
        
        String[] items = {"item 2", "item 4", "item 6", "item 8"};

        // Check that no items are selected.
        assertEquals(true, selectedItems.getItems().isEmpty());
        
        // Check that add is disabled.
        assertEquals(true, add.disabledProperty().get());
        
        // Select 4 items.
        for (String item : items)
            availableItems.getSelectionModel().select(item);
        
        // Check that add is now enabled.
        assertEquals(false, add.disabledProperty().get());
        
        clickOn("#addButton");
        
        // Check that the items were added.
        for (String item : items)
            assertEquals(false, availableItems.getItems().contains(item));
        for (String item : items)
            assertEquals(true, selectedItems.getItems().contains(item));
        
        // Check that clear is not disabled.
        assertEquals(false, clear.disabledProperty().get());
        
        clickOn("#clearButton");
        
        // Check that no items are selected.
        assertEquals(true, selectedItems.getItems().isEmpty());
        
        // Check that the items were removed.
        for (String item : items)
            assertEquals(false, selectedItems.getItems().contains(item));
        
        // Check that the items were returned.
        for (String item : items)
            assertEquals(true, availableItems.getItems().contains(item));
    }
}
