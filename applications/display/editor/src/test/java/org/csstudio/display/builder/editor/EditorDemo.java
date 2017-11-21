/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import org.csstudio.display.builder.model.ModelPlugin;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@SuppressWarnings("nls")
public class EditorDemo extends Application
{
    private static String display_file = "../model/src/main/resources/examples/01_main.bob";
    private EditorGUI editor;

    /** JavaFX main
     *  @throws Exception
     */
    public static void main(final String[] args) throws Exception
    {
        if (args.length == 1)
            display_file = args[0];

        LogManager.getLogManager().readConfiguration(new FileInputStream("../../../phoebus-product/src/main/resources/logging.properties"));

        launch(args);
    }

    /** JavaFX Start */
    @Override
    public void start(final Stage stage)
    {
        // Call ModelPlugin to trigger its static loading of config file..
        ModelPlugin.logger.fine("Load configuration files");


        editor = new EditorGUI();


        stage.setTitle("Editor");
        stage.setWidth(1200);
        stage.setHeight(600);
        final Scene scene = new Scene(editor.getParentNode(), 1200, 600);
        stage.setScene(scene);
        EditorUtil.setSceneStyle(scene);

        // If ScenicView.jar is added to classpath, open it here
        //ScenicView.show(scene);

        stage.show();



        // .. before the model is loaded which may then use predefined colors etc.
        editor.loadModel(new File(display_file));
        stage.setOnCloseRequest((WindowEvent event) -> editor.dispose());
    }
}
