/*
 * Copyright (C) 2019 European Spallation Source ERIC.
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
 *
 */

package org.phoebus.logbook.olog.ui.write;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.phoebus.framework.util.IOUtils;
import org.phoebus.logbook.olog.ui.AttachmentsPreviewController;
import org.phoebus.logbook.olog.ui.Messages;
import org.phoebus.olog.es.api.model.OlogAttachment;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Screenshot;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AttachmentsViewController {

    @FXML
    private Button removeButton;

    @FXML
    private VBox root;

    private TextArea textArea;

    @FXML
    private AttachmentsPreviewController attachmentsPreviewController;

    private LogEntryModel model;

    private boolean autoExpand;

    public AttachmentsViewController() {
        model = new LogEntryModel();
    }

    public AttachmentsViewController(LogEntryModel logEntryModel, Boolean autoExpand) {
        this.autoExpand = autoExpand;
        this.model = logEntryModel;
    }

    @FXML
    public void initialize() {

        removeButton.setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));

        /*
        removeButton.disableProperty().bind(Bindings.isEmpty(attachmentListView.getSelectionModel().getSelectedItems()));

        if (!model.getAttachments().isEmpty()) {
            attachmentListView.getSelectionModel().select(0);
        }

         */
    }

    public void setImages(ObservableList<Image> images) {
        System.out.println();
    }

    @FXML
    public void addFiles() {
        final FileChooser addImageDialog = new FileChooser();
        addImageDialog.setInitialDirectory(new File(System.getProperty("user.home")));
        final List<File> files = addImageDialog.showOpenMultipleDialog(root.getParent().getScene().getWindow());
        if (files == null) { // User cancels file selection
            return;
        }
        addFiles(files);
    }

    @FXML
    public void addCssWindow() {
        Image image = Screenshot.imageFromNode(DockPane.getActiveDockPane());
        addImage(image);
    }

    @FXML
    public void addClipboardContent() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasFiles()) {
            addFiles(clipboard.getFiles());
        } else if (clipboard.hasImage()) {
            Image image = clipboard.getImage();
            addImage(image);
        } else {
            final Alert alert = new Alert(AlertType.INFORMATION);
            alert.setHeaderText(Messages.NoClipboardContent);
            DialogHelper.positionDialog(alert, root.getParent(), -300, -200);
            alert.showAndWait();
        }
    }

    @FXML
    public void removeFiles() {
        //ObservableList<OlogAttachment> selectedAttachments = attachmentListView.getSelectionModel().getSelectedItems();
        //model.removeAttachments(selectedAttachments);
    }

    @FXML
    public void embedImage() {
        EmbedImageDialog embedImageDialog = new EmbedImageDialog();
        Optional<EmbedImageDescriptor> descriptor = embedImageDialog.showAndWait();
        if (descriptor.isPresent()) {
            // Insert markup at caret position. At this point an id must be set.
            int caretPosition = textArea.getCaretPosition();
            String id = UUID.randomUUID().toString();
            String imageMarkup =
                    "![](attachment/" + id + ")"
                            + "{width=" + descriptor.get().getWidth()
                            + " height=" + descriptor.get().getHeight() + "} ";
            textArea.insertText(caretPosition, imageMarkup);
            addImage(descriptor.get().getImage(), id);
        }
    }

    /**
     * Sets a reference to the edit text area such that generated markup for embedded image can be added.
     * TODO: this is a bit ugly, it would maybe better to merge fxmls into a single layout and controller.
     *
     * @param textArea
     */
    public void setTextArea(TextArea textArea) {
        this.textArea = textArea;
    }

    public List<Image> getImages() {
        return Collections.emptyList();
    }

    public List<File> getFiles() {
        return Collections.emptyList();
    }


    private void addFiles(List<File> files) {
        MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
        List<OlogAttachment> attachments = new ArrayList<>();
        for (File file : files) {
            OlogAttachment ologAttachment = new OlogAttachment();
            ologAttachment.setFile(file);
            ologAttachment.setFileName(file.getName());
            String mimeType = fileTypeMap.getContentType(file.getName());
            if (mimeType.startsWith("image")) {
                ologAttachment.setContentType("image");
            } else {
                ologAttachment.setContentType("file");
            }
            attachments.add(ologAttachment);
        }
        model.addAttachments(attachments);
        //attachmentListView.getSelectionModel().clearSelection();
        //attachmentListView.getSelectionModel().select(attachments.get(0));
    }

    private void addImage(Image image) {
        addImage(image, UUID.randomUUID().toString());
    }

    private void addImage(Image image, String id) {
        try {
            File imageFile = new File(System.getProperty("java.io.tmpdir"), id + ".png");
            imageFile.deleteOnExit();
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", imageFile);
            OlogAttachment ologAttachment = new OlogAttachment(id);
            ologAttachment.setContentType("image");
            ologAttachment.setFile(imageFile);
            ologAttachment.setFileName(imageFile.getName());
            model.addAttachment(ologAttachment, true);
            //attachmentListView.getSelectionModel().clearSelection();
            //attachmentListView.getSelectionModel().select(ologAttachment);
        } catch (IOException e) {
            Logger.getLogger(AttachmentsViewController.class.getName())
                    .log(Level.INFO, "Unable to create temp file from clipboard image or embedded image", e);
        }
    }
}
