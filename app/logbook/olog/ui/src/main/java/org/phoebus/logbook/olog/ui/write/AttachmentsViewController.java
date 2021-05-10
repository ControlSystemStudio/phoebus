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

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.olog.ui.AttachmentsPreviewController;
import org.phoebus.logbook.olog.ui.Messages;
import org.phoebus.olog.es.api.model.OlogAttachment;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Screenshot;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AttachmentsViewController {

    @FXML
    private Button removeButton;

    @FXML
    private Button embedSelectedButton;

    @FXML
    private VBox root;

    private TextArea textArea;

    @FXML
    private AttachmentsPreviewController attachmentsPreviewController;

    private LogEntryModel model;

    private boolean autoExpand;

    /**
     * List of attachments selected in the preview's {@link ListView}.
     */

    private ObservableList<Attachment> selectedAttachments = FXCollections.observableArrayList();
    private SimpleBooleanProperty imageAttachmentSelected = new SimpleBooleanProperty(false);


    public AttachmentsViewController(LogEntryModel logEntryModel, Boolean autoExpand) {
        this.autoExpand = autoExpand;
        this.model = logEntryModel;
    }

    @FXML
    public void initialize() {

        removeButton.setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
        removeButton.disableProperty().bind(Bindings.isEmpty(selectedAttachments));
        attachmentsPreviewController.setAttachments(model.getAttachments());

        attachmentsPreviewController.addListSelectionChangeListener(change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    selectedAttachments.addAll(change.getAddedSubList());
                }
                if (change.wasRemoved()) {
                    selectedAttachments.removeAll(change.getRemoved());
                }
            }
            // Enable "Embed Selected" button only if exactly one image attachment is selected.
            imageAttachmentSelected.set(selectedAttachments.size() == 1 &&
                    selectedAttachments.get(0).getContentType().toLowerCase().startsWith("image"));
        });

        embedSelectedButton.disableProperty().bind(imageAttachmentSelected.not());
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
        selectedAttachments.stream().forEach(a -> {
            if(a.getContentType().startsWith("image")){
                String markup = textArea.getText();
                String newMarkup = removeImageMarkup(markup, a.getId());
                textArea.textProperty().set(newMarkup);
            }
        });
        model.removeAttachments(selectedAttachments);

    }

    @FXML
    public void embedImage() {
        EmbedImageDialog embedImageDialog = new EmbedImageDialog();
        Optional<EmbedImageDescriptor> descriptor = embedImageDialog.showAndWait();
        if (descriptor.isPresent()) {
            String id = UUID.randomUUID().toString();
            addEmbeddedImageMarkup(id, descriptor.get().getWidth(), descriptor.get().getHeight());
            addImage(descriptor.get().getImage(), id);
        }
    }

    @FXML
    public void embedSelected(){
        // Just in case... launch dialog only if the first item in the selection is an image
        if(selectedAttachments.get(0).getContentType().toLowerCase().startsWith("image")){
            EmbedImageDialog embedImageDialog = new EmbedImageDialog();
            embedImageDialog.setFile(selectedAttachments.get(0).getFile());
            Optional<EmbedImageDescriptor> descriptor = embedImageDialog.showAndWait();
            if (descriptor.isPresent()) {
                String id = UUID.randomUUID().toString();
                selectedAttachments.get(0).setId(id);
                addEmbeddedImageMarkup(id, descriptor.get().getWidth(), descriptor.get().getHeight());
            }
        }
    }

    private void addEmbeddedImageMarkup(String id, int width, int height){
        // Insert markup at caret position. At this point an id must be set.
        int caretPosition = textArea.getCaretPosition();
        String imageMarkup =
                "![](attachment/" + id + ")"
                        + "{width=" + width
                        + " height=" + height + "} ";
        textArea.insertText(caretPosition, imageMarkup);
    }

    protected String removeImageMarkup(String markup, String imageId){
        int index = markup.indexOf(imageId);
        if(index == -1){
            return markup;
        }

        String stringBefore = markup.substring(0, index);
        String stringAfter = markup.substring(index + imageId.length());

        int exclamationMarkIndex = stringBefore.lastIndexOf('!');
        int closingCurlyBraceIndex = stringAfter.indexOf('}');

        return markup.substring(0, exclamationMarkIndex) +
                markup.substring((stringBefore + imageId).length() + closingCurlyBraceIndex + 1);
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
        } catch (IOException e) {
            Logger.getLogger(AttachmentsViewController.class.getName())
                    .log(Level.INFO, "Unable to create temp file from clipboard image or embedded image", e);
        }
    }
}
