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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.phoebus.framework.util.IOUtils;
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
    private TitledPane titledPane;

    @FXML
    private StackPane previewPane;

    @FXML
    private ImageView imagePreview;

    @FXML
    private TextArea textPreview;

    @FXML
    private ListView<OlogAttachment> attachmentListView;


    @FXML
    private Button removeButton;

    @FXML
    private GridPane noPreviewPane;

    @FXML
    private VBox root;

    @FXML
    private TextArea textArea;

    private LogEntryModel model;

    private boolean autoExpand;


    public AttachmentsViewController(LogEntryModel logEntryModel, Boolean autoExpand) {
        this.autoExpand = autoExpand;
        this.model = logEntryModel;
    }

    @FXML
    public void initialize() {

        // Open/close the attachments pane if there's something to see resp. not
        if (autoExpand && (!this.model.getAttachments().isEmpty())) {
            Platform.runLater(() ->
            {
                titledPane.setExpanded(true);
            });
        }
        attachmentListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        attachmentListView.setCellFactory(view -> new AttachmentRow());
        attachmentListView.setItems(model.getAttachments());
        attachmentListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<OlogAttachment>() {
            @Override
            public void changed(ObservableValue<? extends OlogAttachment> observable, OlogAttachment oldValue, OlogAttachment newValue) {
                showPreview(newValue);
            }
        });

        removeButton.setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
        removeButton.disableProperty().bind(Bindings.isEmpty(attachmentListView.getSelectionModel().getSelectedItems()));

        if(!model.getAttachments().isEmpty()){
            attachmentListView.getSelectionModel().select(0);
        }
    }

    public void setImages(ObservableList<Image> images) {
        if (autoExpand && !images.isEmpty()) {
            Platform.runLater(() ->
            {
                titledPane.setExpanded(true);
            });
        }
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
        ObservableList<OlogAttachment> selectedAttachments = attachmentListView.getSelectionModel().getSelectedItems();
        model.removeAttachments(selectedAttachments);
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
        attachmentListView.getSelectionModel().clearSelection();
        attachmentListView.getSelectionModel().select(attachments.get(0));
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
            attachmentListView.getSelectionModel().clearSelection();
            attachmentListView.getSelectionModel().select(ologAttachment);
        } catch (IOException e) {
            Logger.getLogger(AttachmentsViewController.class.getName())
                    .log(Level.INFO, "Unable to create temp file from clipboard image or embedded image", e);
        }
    }

    /**
     * Shows selected attachment in preview pane.
     * @param ologAttachment
     */
    private void showPreview(OlogAttachment ologAttachment){
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
     * @param ologAttachment
     */
    private void showImagePreview(OlogAttachment ologAttachment){
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
     * @param ologAttachment
     */
    private void showFilePreview(OlogAttachment ologAttachment){
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
