/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.dialog;

import java.util.List;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** Demo of the {@link ListPickerDialog}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ListPickerDialogDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage)
    {
        final Button button = new Button("Open Dialog");
        final Scene scene = new Scene(new BorderPane(button), 400, 300);
        stage.setScene(scene);
        stage.show();

        button.setOnAction(event ->
        {
            final Dialog<String> dialog = new ListPickerDialog(button, List.of("Apple", "Orange"), "Orange");
            dialog.setTitle("Open");
            dialog.setHeaderText("Select application for opening\nthe item");
            System.out.println(dialog.showAndWait());
        });
    }

    public static void main(String[] args)
    {
        launch(ListPickerDialogDemo.class, args);
    }
}
