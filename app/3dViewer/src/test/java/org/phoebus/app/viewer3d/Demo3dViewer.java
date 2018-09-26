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

import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Demo3dViewer extends Application
{
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        TextField textField = new TextField();
        Button fileButton = new Button(null, ImageCache.getImageView(ImageCache.class, "/icons/fldr_obj.png"));
        FileChooser chooser = new FileChooser();
        HBox toolbar = new HBox();
        toolbar.getChildren().addAll(textField, fileButton);
        Viewer3d viewer = new Viewer3d();
        VBox root = new VBox();
        Insets insets = new Insets(10);
        
        VBox.setMargin(viewer, insets);
        
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Shape files (.shp)", "*.shp");
        chooser.getExtensionFilters().add(extFilter);

        fileButton.setOnAction(event -> 
        {
            File file = chooser.showOpenDialog(primaryStage);
            if (null != file)
            {
                textField.setText(file.getPath());
                viewer.buildStructure(file);
            }
        });
        
        HBox.setHgrow(textField, Priority.ALWAYS);
        toolbar.setSpacing(10);
        toolbar.setPadding(insets);
        root.getChildren().addAll(toolbar, viewer);
        
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
