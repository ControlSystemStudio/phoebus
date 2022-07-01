/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.dialog;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.util.ArrayList;

/**
 * Demonstrate the ListSelectionDialog
 * @author Evan Smith
 */
public class ListSelectionPopOverDemo extends ApplicationWrapper
{
    ObservableList<String> available;
    ObservableList<String> selected;

    public static void main(String[] args)
    {
        launch(ListSelectionPopOverDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        available = FXCollections.observableArrayList();
        selected  = FXCollections.observableArrayList();
        
        for (int i = 0; i < 50; i++)
            available.add("item " + i);
        
        StackPane root = new StackPane();

        ListSelectionPopOver listSelectionPopOver = ListSelectionPopOver.create(
                (items, popOver) -> {
                    selected.setAll(items);
                    if(popOver.isShowing()) {
                        popOver.hide();
                    }
                },
                (items, popOver) -> {
                    popOver.hide();
                }
        );
        listSelectionPopOver.setAvailable(available, new ArrayList<>());

        HBox container = new HBox();
        TextField textField = new TextField();
        textField.textProperty().bind(Bindings.createStringBinding(
                () -> String.join(",", selected),
                selected
        ));
        Button button = new Button("show popover");
        button.setOnAction(action -> {
            listSelectionPopOver.setAvailable(available, selected);
            listSelectionPopOver.show(button);
        });
        container.getChildren().addAll(textField, button);
        root.getChildren().add(container);
        Scene scene = new Scene(root, 200, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

}
