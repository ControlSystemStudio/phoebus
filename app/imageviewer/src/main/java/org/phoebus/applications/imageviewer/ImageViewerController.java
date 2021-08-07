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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public void setImage(URL url) {
        try {
            BufferedImage bufferedImage = ImageIO.read(url);
            Image image;
            if(url.toExternalForm().endsWith("svg")){
                image = SVGHelper.loadSVG(url.openStream(), 0, 0);
            }
            else{
                image = SwingFXUtils.toFXImage(bufferedImage, null);
            }
            imageView.setImage(image);
        } catch (Exception e) {
            Logger.getLogger(ImageViewerController.class.getName())
                    .log(Level.SEVERE, "Unable to load image to image viewer", e);
        }
    }
}
