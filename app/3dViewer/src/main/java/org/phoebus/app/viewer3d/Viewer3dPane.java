/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.app.viewer3d;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
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
 * <i>.shp</i> files from the file system or URLs.
 * 
 * @author Evan Smith
 *
 */
public class Viewer3dPane extends VBox
{
    public final static Logger logger = Logger.getLogger(Viewer3dPane.class.getName());
    
    public Viewer3dPane() throws Exception
    {
        super();
        
        Insets insets = new Insets(10);

        TextField textField = new TextField();
        Button fileButton = new Button(null, ImageCache.getImageView(ImageCache.class, "/icons/fldr_obj.png"));
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Shape files (.shp)", "*.shp");
        
        HBox toolbar = new HBox();
        
        Viewer3d viewer = new Viewer3d();

        fileChooser.getExtensionFilters().add(extFilter);
        toolbar.getChildren().addAll(textField, fileButton);

        VBox.setVgrow(viewer, Priority.ALWAYS);
        VBox.setMargin(viewer, insets);        

        fileButton.setOnAction(event -> 
        {   
            
            File file = fileChooser.showOpenDialog(getScene().getWindow());
            
            if (null != file)
            {
                try
                {
                    String resource = file.toURI().toURL().toString();
                    textField.setText(resource);
                    loadResource(resource, viewer);
                } 
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    logger.log(Level.WARNING, "Loading Resource failed", ex);
                }
            }
        });
        
        HBox.setHgrow(textField, Priority.ALWAYS);
        toolbar.setSpacing(10);
        toolbar.setPadding(insets);
        
        textField.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
            {
                String resource = textField.getText();
                loadResource(resource, viewer);
            }
        });
        
        getChildren().addAll(toolbar, viewer);
    }
    
    /**
     * Load a resource file and update the viewer.
     * @param resource
     * @param viewer
     */
    private void loadResource(final String resource, Viewer3d viewer)
    {
        JobManager.schedule("Read 3d viewer resource", monitor -> 
        {
            InputStream inputStream = null;
            try
            {
                inputStream = ResourceUtil.openResource(resource);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Opening resource '" + resource + "' failed", ex);
            }
            
            try
            {
                final Xform struct = viewer.buildStructure(inputStream);
                if (null != struct)
                    Platform.runLater(() -> viewer.setStructure(struct));
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Building structure failed", ex);
            }
        });
    }
}
