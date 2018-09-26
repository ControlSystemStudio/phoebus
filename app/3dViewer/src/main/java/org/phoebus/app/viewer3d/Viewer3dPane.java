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

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * VBox that wraps a Viewer3d instance and adds support for loading
 * .shp files.
 * 
 * @author Evan Smith
 *
 */
public class Viewer3dPane extends VBox
{
    public Viewer3dPane() throws Exception
    {
        super();
        
        Insets insets = new Insets(10);

        TextField pathField = new TextField();
        Button fileButton = new Button(null, ImageCache.getImageView(ImageCache.class, "/icons/fldr_obj.png"));
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Shape files (.shp)", "*.shp");
        
        HBox toolbar = new HBox();
        
        Viewer3d viewer = new Viewer3d();

        fileChooser.getExtensionFilters().add(extFilter);
        toolbar.getChildren().addAll(pathField, fileButton);

        VBox.setVgrow(viewer, Priority.ALWAYS);
        VBox.setMargin(viewer, insets);        

        fileButton.setOnAction(event -> 
        {   
            
            File file = fileChooser.showOpenDialog(getScene().getWindow());
            
            if (null != file)
            {
                pathField.setText(file.getPath());
                viewer.buildStructure(file);
            }
        });
        
        HBox.setHgrow(pathField, Priority.ALWAYS);
        toolbar.setSpacing(10);
        toolbar.setPadding(insets);
        
        /* TODO Rework this. Study how the display runtime does it with resource files in ModelResourceUtil.java */
        pathField.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
            {
                String pathway = pathField.getText();
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
        
        getChildren().addAll(toolbar, viewer);
    }
}
