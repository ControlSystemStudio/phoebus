/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.imageviewer;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.svg.SVGTranscoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the image viewer application UI. It intentionally does not
 * use the {@link org.phoebus.ui.javafx.ImageCache} as the intention is to render
 * images at original - potentially very high - resolution.
 */
public class ImageViewerController {

    @FXML
    private Node root;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private ImageView imageView;

    @FXML
    public void initialize() {
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
    }

    public Node getRoot() {
        return root;
    }

    /**
     * Sets the image in the view. Shows error dialog if the image file
     * cannot be loaded/rendered, e.g. non-image file or incompatible SVG.
     * @param url
     */
    public void setImage(URL url) {
        try {
            Image image;
            if(url.toExternalForm().endsWith("svg")){
                image = SVGTranscoder.loadSVG(url.openStream(), 0, 0);
            }
            else{
                BufferedImage bufferedImage = ImageIO.read(url);
                if(bufferedImage == null){
                    throw new RuntimeException("Failed to create image from URL " + url.toExternalForm());
                }
                image = SwingFXUtils.toFXImage(bufferedImage, null);
            }
            imageView.setImage(image);
        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(root,
                    Messages.ErrorDialogTitle,
                    MessageFormat.format(Messages.ErrorDialogText,url.toExternalForm()),
                    e);
            Logger.getLogger(ImageViewerController.class.getName())
                    .log(Level.SEVERE, "Unable to load image to image viewer", e);
        }
    }
}
