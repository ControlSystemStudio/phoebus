/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Demonstrate the LogEntryDialog
 * @author Evan Smith
 */
public class DemoLogEntryDialog extends Application
{

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        LogEntryDialog logEntryDialog = new LogEntryDialog(root, null);
        logEntryDialog.showAndWait();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
    
}
