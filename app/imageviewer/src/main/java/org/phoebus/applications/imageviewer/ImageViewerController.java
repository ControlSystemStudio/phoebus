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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
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
 * <p>
 * A button is available to scale the image to the current view size.
 */
public class ImageViewerController {

    @FXML
    private Node root;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private ImageView imageView;

    @FXML
    private Pane watermarkPane;

    @FXML
    private Button scaleToFitButton;

    @FXML
    private Label watermarkText;

    @FXML
    private BorderPane imageParent;

    private Image image;

    private SimpleBooleanProperty isSVG = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty fitToWindow = new SimpleBooleanProperty(false);

    private SimpleBooleanProperty showWatermarkProperty = new SimpleBooleanProperty(true);

    private SimpleObjectProperty fontProperty = new SimpleObjectProperty();

    @FXML
    public void initialize() {
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scaleToFitButton.setOnAction(e -> {
            fitToWindow.set(!fitToWindow.get());
            scaleToFitButton.setText(fitToWindow.get() ? Messages.OneHundredPercent : Messages.ScaleToFit);
            scale();
        });
        scaleToFitButton.disableProperty().bind(isSVG);

        scrollPane.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (fitToWindow.get()) {
                scale();
            }
        });

        scrollPane.heightProperty().addListener((observable, oldValue, newValue) -> {
            if (fitToWindow.get()) {
                scale();
            }
        });

        fontProperty.set(new Font(20));

        watermarkPane.visibleProperty().bind(showWatermarkProperty);
        watermarkText.fontProperty().bind(fontProperty);

        watermarkText.getStylesheets().addAll(getClass().getResource("/style.css").toExternalForm());
        watermarkText.getStyleClass().add("outline");
    }

    public Node getRoot() {
        return root;
    }

    /**
     * Sets the image in the view. Shows error dialog if the image file
     * cannot be loaded/rendered, e.g. non-image file or incompatible SVG.
     *
     * @param url
     */
    public void setImage(URL url) {
        try {
            if (url.toExternalForm().endsWith("svg")) {
                image = SVGTranscoder.loadSVG(url.openStream(), 0, 0);
                isSVG.set(true);
            } else {
                BufferedImage bufferedImage = ImageIO.read(url);
                if (bufferedImage == null) {
                    throw new RuntimeException("Failed to create image from URL " + url.toExternalForm());
                }
                image = SwingFXUtils.toFXImage(bufferedImage, null);
            }
            imageView.setImage(image);
            imageParent.layout();
            scale();

        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(root,
                    Messages.ErrorDialogTitle,
                    MessageFormat.format(Messages.ErrorDialogText, url.toExternalForm()),
                    e);
            Logger.getLogger(ImageViewerController.class.getName())
                    .log(Level.SEVERE, "Unable to load image to image viewer", e);
        }
    }

    /**
     * Rescales image as requested, aspect ratio is preserved.
     */
    private void scale() {
        if (fitToWindow.get()) {
            double scalingFactor =
                    Math.min(scrollPane.widthProperty().get() / image.getWidth(), scrollPane.heightProperty().get() / image.getHeight());
            imageView.setFitWidth(image.getWidth() * scalingFactor - 20);
            imageView.setFitHeight(image.getHeight() * scalingFactor - 20);
        } else {
            imageView.setFitWidth(image.getWidth());
            imageView.setFitHeight(image.getHeight());
        }
        fontProperty.set(getFont());
    }

    public void toggleWatermark() {
        showWatermarkProperty.set(!showWatermarkProperty.getValue());
    }

    private Font getFont() {
        double height = imageView.getFitHeight();
        double width = imageView.getFitWidth();
        return Font.font("Liberation Sans", FontWeight.BOLD, FontPosture.REGULAR, Math.min(width, height) / 10);
    }
}
