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
 * Demonstrate the {@link LogEntryEditorStage}
 * @author Evan Smith
 *
 * <b>NOTE</b> On Java 11+ the run configuration must include JVM options for the JavaFX modules, e.g.
 * --module-path
 * /Users/georgweiss/.m2/repository/org/openjfx/javafx-base/14:/Users/georgweiss/.m2/repository/org/openjfx/javafx-graphics/14:/Users/georgweiss/.m2/repository/org/openjfx/javafx-fxml/14:/Users/georgweiss/.m2/repository/org/openjfx/javafx-controls/14
 * --add-modules=javafx.base
 * --add-modules=javafx.graphics
 * --add-modules=javafx.fxml
 * --add-modules=javafx.controls
 */
public class DemoLogEntryEditorStage extends Application
{

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        new LogEntryEditorStage(root, new LogEntryModel(), null).show();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
    
}
