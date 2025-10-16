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

package org.phoebus.logbook.olog.ui.write;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import org.phoebus.logbook.olog.ui.Messages;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the UI used to embed image markup to the log entry description.
 */
public class EmbedImageDialogController implements Initializable{

    @SuppressWarnings("unused")
    @FXML
    private DialogPane dialogPane;
    @SuppressWarnings("unused")
    @FXML
    private Label fileLabel;
    @SuppressWarnings("unused")
    @FXML
    private Label widthLabel;
    @SuppressWarnings("unused")
    @FXML
    private Label heightLabel;
    @SuppressWarnings("unused")
    @FXML
    private Button browseButton;
    @SuppressWarnings("unused")
    @FXML
    private Button clipboardButton;
    @SuppressWarnings("unused")
    @FXML
    private TextField fileName;
    @SuppressWarnings("unused")
    @FXML
    private TextField width;
    @SuppressWarnings("unused")
    @FXML
    private TextField height;
    @SuppressWarnings("unused")
    @FXML
    private Label scaleLabel;
    @SuppressWarnings("unused")
    @FXML
    private TextField scale;

    /**
     * This is set when image is selected from file or clipboard. It is not bound to
     * a UI component.
     */
    private final IntegerProperty widthProperty = new SimpleIntegerProperty();
    /**
     * This is the computed width value rendered in the UI component.
     */
    private final IntegerProperty scaledWidthProperty = new SimpleIntegerProperty();
    /**
     * This is set when image is selected from file or clipboard. It is not bound to
     * a UI component.
     */
    private final IntegerProperty heightProperty = new SimpleIntegerProperty();
    /**
     * This is the computed width value rendered in the UI component.
     */
    private final IntegerProperty scaledHeightProperty = new SimpleIntegerProperty();
    private final SimpleStringProperty filenameProperty = new SimpleStringProperty();
    private final DoubleProperty scaleProperty = new SimpleDoubleProperty(1.0);

    private String id;

    private Image image;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){

        // Clipboard button enabled only if there is an image on the clipboard.
        clipboardButton.disableProperty().set(!Clipboard.getSystemClipboard().hasImage());

        StringConverter<? extends Number> doubleStringConverter = new DoubleStringConverter();
        Bindings.bindBidirectional(scale.textProperty(), scaleProperty, (StringConverter<Number>) doubleStringConverter);

        // Compute width based on original size and scaling factor.
        scaledWidthProperty.bind(scaleProperty.multiply(widthProperty));
        // Compute height based on original size and scaling factor.
        scaledHeightProperty.bind(scaleProperty.multiply(heightProperty));

        fileName.textProperty().bind(filenameProperty);
        width.textProperty().bind(scaledWidthProperty.asString());
        height.textProperty().bind(scaledHeightProperty.asString());

        BooleanBinding okButtonBinding =
                scaledWidthProperty.lessThanOrEqualTo(0)
                        .or(scaleProperty.lessThanOrEqualTo(0));
        dialogPane.lookupButton(ButtonType.OK).disableProperty().bind(okButtonBinding);
    }

    @SuppressWarnings("unused")
    @FXML
    public void browse(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(Messages.SelectFile);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image files (jpg, jpeg, png, gif)", "*.jpg", "*.jpeg", "*.png", "*.gif")

        );

        File file = fileChooser.showOpenDialog(dialogPane.getScene().getWindow());
        if(file != null){
            filenameProperty.set(file.getName());
            try {
                BufferedImage bufferedImage = ImageIO.read(file);
                image = SwingFXUtils.toFXImage(bufferedImage, null);
                widthProperty.set((int)Math.round(image.getWidth()));
                heightProperty.set((int)Math.round(image.getHeight()));
                id = UUID.randomUUID().toString();
            } catch (IOException ex) {
                Logger.getLogger(EmbedImageDialogController.class.getName())
                        .log(Level.SEVERE, "Unable to load image file " + file.getAbsolutePath(), ex);
            }
        }
    }

    @SuppressWarnings("unused")
    @FXML
    public void pasteClipboard(){
        image = Clipboard.getSystemClipboard().getImage();
        widthProperty.set((int)Math.round(image.getWidth()));
        heightProperty.set((int)Math.round(image.getHeight()));
        id = UUID.randomUUID().toString();
        filenameProperty.set(id);
    }

    public void setFile(File file){
        filenameProperty.set(file.getName());
        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            image = SwingFXUtils.toFXImage(bufferedImage, null);
            widthProperty.set((int)Math.round(image.getWidth()));
            heightProperty.set((int)Math.round(image.getHeight()));
            id = UUID.randomUUID().toString();
        } catch (IOException ex) {
            Logger.getLogger(EmbedImageDialogController.class.getName())
                    .log(Level.SEVERE, "Unable to load image file " + file.getAbsolutePath(), ex);
        }
    }

    /**
     * @return A {@link EmbedImageDescriptor} that will carry the data
     * required to create an image attachment for the log entry.
     */
    public EmbedImageDescriptor getEmbedImageDescriptor(){
        EmbedImageDescriptor embedImageDescriptor = new EmbedImageDescriptor();
        embedImageDescriptor.setFileName(filenameProperty.get());
        embedImageDescriptor.setImage(image);
        embedImageDescriptor.setWidth(scaledWidthProperty.get());
        embedImageDescriptor.setHeight(scaledHeightProperty.get());
        return embedImageDescriptor;
    }
}
