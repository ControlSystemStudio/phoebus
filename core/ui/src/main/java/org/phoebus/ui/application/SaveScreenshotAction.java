/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Screenshot;
import org.phoebus.util.time.TimestampFormats;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;

/**
 * Context menu item to save a screen shot of a JavaFX node.
 * @author Evan Smith
 */
public class SaveScreenshotAction extends MenuItem
{
    
    public SaveScreenshotAction(Node parent)
    {
        setText("Save Screenshot...");
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/save_edit.png"));
        setOnAction(event -> 
        {
            // Take the screenshot.
            
            BufferedImage bufImage = Screenshot.bufferFromNode(parent);

            // Put file IO on background thread.
            
            JobManager.schedule("Take Screenshot", monitor ->
            {
    
                // Recommended file name and location.
                
                Instant now = Instant.now();
                File imageFile = new File(new File(System.getProperty("user.home")), "Screenshot-" + TimestampFormats.SECONDS_FORMAT.format(now) + ".png");
                
                // Open file dialog to choose the file.
    
                SaveAsDialog saveImageDialog = new SaveAsDialog();
                imageFile = saveImageDialog.promptForFile(parent.getScene().getWindow(), "Save Screenshot", imageFile, null);
                
                // Write to the file.
                
                try
                {
                    ImageIO.write(bufImage, "png", imageFile);
                } 
                catch (IOException ex)
                {
                    logger.log(Level.WARNING, "Saving screenshot failed.", ex);
                }
                
            });
        });
    }
}
