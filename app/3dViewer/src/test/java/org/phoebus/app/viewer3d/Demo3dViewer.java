/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.app.viewer3d;

import java.io.File;
import java.net.URL;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Demo3dViewer extends Application
{

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Viewer3d viewer = new Viewer3d();
        TextField textField = new TextField();
        VBox root = new VBox();
        Insets insets = new Insets(10);
        
        VBox.setMargin(textField, insets);
        VBox.setMargin(viewer, insets);
        
        root.getChildren().addAll(textField, viewer);
        
        textField.setOnKeyPressed(event -> 
        {
            if (event.getCode() == KeyCode.ENTER)
            {
                String pathway = textField.getText();
                if (pathway.startsWith("examples:"))
                {
                    pathway = pathway.replaceFirst("examples:", "");
                    URL resource = Viewer3d.class.getResource(pathway);
                    if (null != resource)
                        pathway = resource.getFile();
                }
                File file = new File(pathway);
                if (file.exists() && !file.isDirectory())
                    viewer.buildStructure(file);
            }
        });
        
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args)
    {
        launch(args);
    }

}
