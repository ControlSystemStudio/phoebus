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

package org.phoebus.logbook.olog.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.phoebus.framework.util.IOUtils;
import org.phoebus.logbook.olog.ui.write.AttachmentsViewController;
import org.phoebus.logbook.olog.ui.write.LogEntryModel;
import org.phoebus.olog.es.api.model.OlogAttachment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the AttachmentPreview.fxml view. It is designed to be used both
 * by log entry editor as well as the read-only log entry details view.
 */
public class AttachmentsPreviewController {

    @FXML
    private StackPane previewPane;

    @FXML
    private ImageView imagePreview;

    @FXML
    private TextArea textPreview;

    @FXML
    private GridPane noPreviewPane;

    @FXML
    private ListView<OlogAttachment> attachmentListView;

    private LogEntryModel model;

    public AttachmentsPreviewController() {
        model = new LogEntryModel();
    }

    public AttachmentsPreviewController(LogEntryModel logEntryModel) {
        this.model = model;
    }

    @FXML
    public void initialize() {

        attachmentListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        attachmentListView.setCellFactory(view -> new AttachmentRow());
        attachmentListView.setItems(model.getAttachments());
        attachmentListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<OlogAttachment>() {
            @Override
            public void changed(ObservableValue<? extends OlogAttachment> observable, OlogAttachment oldValue, OlogAttachment newValue) {
                showPreview(newValue);
            }
        });


    }

    private class AttachmentRow extends ListCell<OlogAttachment> {
        @Override
        public void updateItem(OlogAttachment attachment, boolean empty) {
            super.updateItem(attachment, empty);
            if (empty)
                setText("");
            else {
                setText(attachment.getName());
            }
        }
    }

    /**
     * Shows selected attachment in preview pane.
     *
     * @param ologAttachment
     */
    private void showPreview(OlogAttachment ologAttachment) {
        if (ologAttachment == null) {
            imagePreview.visibleProperty().setValue(false);
            textPreview.visibleProperty().setValue(false);
            return;
        }
        if (ologAttachment.getContentType().equals("image")) {
            showImagePreview(ologAttachment);
        } else {
            // Other file types...
            // Need some file content detection here (Apache Tika?) to determine if the file is
            // plain text and thus possible to preview in a TextArea.
            showFilePreview(ologAttachment);
        }
    }

    /**
     * Shows image preview in preview pane. The size of the {@link ImageView} is calculated based on
     * the size of the preview pane and the actual image size such that the complete image is always shown.
     * TODO: Viewing the image in original resolution should be implemented as a separate action, e.g. double
     * click image attachment in list.
     *
     * @param ologAttachment
     */
    private void showImagePreview(OlogAttachment ologAttachment) {
        try {
            BufferedImage bufferedImage = ImageIO.read(ologAttachment.getFile());
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
            double width = previewPane.widthProperty().getValue();
            double height = previewPane.heightProperty().getValue();
            double imageWidth = image.getWidth();
            double imageHeight = image.getHeight();
            if (imageWidth > imageHeight) {
                double scale = imageWidth / width;
                imagePreview.fitWidthProperty().setValue(width);
                imagePreview.fitHeightProperty().setValue(imageHeight / scale);
            } else {
                double scale = imageHeight / height;
                imagePreview.fitWidthProperty().setValue(imageWidth / scale);
                imagePreview.fitHeightProperty().setValue(height);
            }
            imagePreview.visibleProperty().setValue(true);
            textPreview.visibleProperty().setValue(false);
            imagePreview.setImage(image);
        } catch (IOException ex) {
            Logger.getLogger(AttachmentsViewController.class.getName())
                    .log(Level.SEVERE, "Unable to load image file " + ologAttachment.getFile().getAbsolutePath(), ex);
        }
    }

    /**
     * Shows a file attachment that is not an image, e.g. text.
     * TODO: Some kind of file content detection (Apache Tika?) should be used to determine if preview makes sense.
     *
     * @param ologAttachment
     */
    private void showFilePreview(OlogAttachment ologAttachment) {
        imagePreview.visibleProperty().setValue(false);
        noPreviewPane.visibleProperty().setValue(false);
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            IOUtils.copy(new FileInputStream(ologAttachment.getFile()), result);
            String content = new String(result.toByteArray());
            textPreview.textProperty().set(content);
            textPreview.visibleProperty().setValue(true);
        } catch (IOException e) {
            Logger.getLogger(AttachmentsViewController.class.getName())
                    .log(Level.SEVERE, "Unable to read file " + ologAttachment.getFile().getAbsolutePath(), e);
        }
    }
}
