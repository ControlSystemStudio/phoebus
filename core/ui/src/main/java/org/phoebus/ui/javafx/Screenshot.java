/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;

/** Create screenshot of a JavaFX scene
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Screenshot
{
    private final BufferedImage image;

    /** Initialize screenshot
     *
     *  <p>Must be called on UI thread
     *  @param scene Scene to capture
     *  @throws Exception on error
     */
    public Screenshot(final Scene scene) throws Exception
    {
        image = fromNode(scene.getRoot());
        // Create snapshot file
        //        final WritableImage jfx = scene.snapshot(null);
        //        image = new BufferedImage((int)jfx.getWidth(),
        //                (int)jfx.getHeight(),
        //                BufferedImage.TYPE_INT_ARGB);
        //        SwingFXUtils.fromFXImage(jfx, image);
    }

    public Screenshot(final Node node) throws Exception
    {
        image = fromNode(node);
    }

    public static BufferedImage fromNode(Node node)
    {
        final WritableImage jfx = node.snapshot(null, null);
        final BufferedImage img = new BufferedImage((int)jfx.getWidth(),
                (int)jfx.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        SwingFXUtils.fromFXImage(jfx, img);

        return img;
    }

    /** Write to file
     *  @param file Output file
     *  @throws Exception on error
     */
    public void writeToFile(final File file) throws Exception
    {
        try
        {
            ImageIO.write(image, "png", file);
        }
        catch (Exception ex)
        {
            throw new Exception("Cannot create screenshot " + file.getAbsolutePath(), ex);
        }
    }

    /** Write to temp. file
     *  @param file_prefix File prefix
     *  @return File that was created
     *  @throws Exception on error
     */
    public File writeToTempfile(final String file_prefix) throws Exception
    {
        try
        {
            final File file = File.createTempFile(file_prefix, ".png");
            file.deleteOnExit();
            writeToFile(file);
            return file;
        }
        catch (Exception ex)
        {
            throw new Exception("Cannot create tmp. file:\n" + ex.getMessage());
        }
    }
}
