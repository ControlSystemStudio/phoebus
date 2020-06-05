/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Demonstrate the ListSelectionDialog
 * @author Evan Smith
 */
public class ListSelectionDialogDemo extends ApplicationWrapper
{
    ObservableList<String> available;
    ObservableList<String> selected;

    public static void main(String[] args)
    {
        launch(ListSelectionDialogDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        available = FXCollections.observableArrayList();
        selected  = FXCollections.observableArrayList();
        
        for (int i = 0; i < 50; i++)
            available.add("item " + i);
        
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        
        ListSelectionDialog dialog = new ListSelectionDialog(root, "TEST", this::available, this::selected, this::addSelected, this::removeSelected);
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
        Boolean result = selected.add(item);
        FXCollections.sort(selected);
        return result;
    }
    
    public Boolean removeSelected(String item)
    {
        return selected.remove(item);
    }

}
