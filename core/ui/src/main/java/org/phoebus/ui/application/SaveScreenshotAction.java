/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;

import javax.imageio.ImageIO;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Screenshot;
import org.phoebus.util.time.TimestampFormats;

import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Context menu item to save a screen shot of a JavaFX node.
 * @author Evan Smith
 */
public class SaveScreenshotAction extends MenuItem
{
    private static final ImageView icon = ImageCache.getImageView(ImageCache.class, "/icons/save_edit.png");
    private static final String PNG = "png";
    private static final String dotPNG = ".png";
    
    /**
     * Constructor.
     * @param parent - JavaFX Parent
     * @param filters - File extension filters.
     */
    public SaveScreenshotAction(Parent parent, ExtensionFilter[] filters)
    {
        super(Messages.SaveScreenshot, icon);

        setOnAction(event -> 
        {
            // Take the screenshot.
            
            BufferedImage bufImage = Screenshot.bufferFromNode(parent);

            // Recommended file name.
            
            Instant now = Instant.now();
            final File recommendFile = new File("Screenshot-" + TimestampFormats.SECONDS_FORMAT.format(now) + dotPNG);
            
            // Open file dialog to choose the file.
            
            SaveAsDialog saveImageDialog = new SaveAsDialog();
            final File imageFile = saveImageDialog.promptForFile(parent.getScene().getWindow(), Messages.SaveScreenshotAsFilename, recommendFile, filters);
            
            if (null == imageFile)
                return;

            // Put file IO on background thread.
            
            JobManager.schedule(Messages.SaveScreenshot, monitor ->
            { 
                // Write to the file.
                
                try
                {
                    monitor.beginTask(imageFile.toString());
                    ImageIO.write(bufImage, PNG, imageFile);
                } 
                catch (IOException ex)
                {
                    ExceptionDetailsErrorDialog.openError("Screenshot error", "Cannot write screenshot", ex);
                }
                
            });
        });
    }
}
