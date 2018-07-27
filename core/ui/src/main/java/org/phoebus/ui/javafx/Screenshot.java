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
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;

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
     */
    public Screenshot(final Scene scene)
    {
        image = bufferFromNode(scene.getRoot());
    }

    public Screenshot(final Node node)
    {
        image = bufferFromNode(node);
    }

    public Screenshot(final Image image)
    {
        this.image = bufferFromImage(image);
    }

    /** Get a JavaFX Node Snapshot as a JavaFX Image
     * @param node
     * @return Image
     */
    public static WritableImage imageFromNode(Node node)
    {
        return node.snapshot(null, null);
    }

    /** Get a AWT BufferedImage from JavaFX Image
     *  @param jfx {@link Image}
     *  @return BufferedImage
     */
    public static BufferedImage bufferFromImage(final Image jfx)
    {
        final BufferedImage img = new BufferedImage((int)jfx.getWidth(),
                (int)jfx.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        SwingFXUtils.fromFXImage(jfx, img);

        return img;
    }

    /** Get a JavaFX Node Snapshot as an AWT BufferedImage
     * @param node
     * @return BufferedImage
     */
    public static BufferedImage bufferFromNode(Node node)
    {
        return bufferFromImage(imageFromNode(node));
    }

    /* Commented out because AWT causes issues, but remains as example of how it could be done.
    /**
     * Capture an image of the entire screen.
     * @return Image

    public static Image captureScreen()
    {
        try {
            Robot robot = new Robot();

            // Create an image of the main screen with the retrieved screen dimensions.
            Rectangle screenDimensions = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenCapture = robot.createScreenCapture(screenDimensions);

            return SwingFXUtils.toFXImage(screenCapture, null);
        } catch (AWTException ex) {
            logger.log(Level.WARNING, "Screen capture failed.", ex);
        }
        return null;
    }
    */

    /**
     * Get an image from the clip board.
     * <p> Returns null if no image is on the clip board.
     * @return Image
     */
    public static Image getImageFromClipboard()
    {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        return clipboard.getImage();
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
