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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.olog.ui.AttachmentsViewController;
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
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AttachmentsEditorController {

    @FXML
    private Button removeButton;

    @FXML
    private Button embedSelectedButton;

    @FXML
    private VBox root;

    private TextArea textArea;

    @SuppressWarnings("unused")
    @FXML
    private AttachmentsViewController attachmentsViewController;

    private final Logger logger = Logger.getLogger(AttachmentsEditorController.class.getName());

    private final SimpleBooleanProperty imageAttachmentSelected = new SimpleBooleanProperty(false);

    /**
     * List of (temporary) attachment {@link File}s deleted when log entry has been successfully persisted.
     */
    private final List<File> filesToDeleteAfterSubmit = new ArrayList<>();

    private final LogEntry logEntry;

    @FXML
    private Label sizeLimitsLabel;

    @FXML
    private Label sizesErrorLabel;

    private final SimpleStringProperty sizesErrorMessage = new SimpleStringProperty();

    /**
     * Max size in MB of a single attachment. Default is 15.
     */
    private double maxFileSize = 15.0;
    /**
     * Max total size in MB for log entry, attachments included. Default is 50.
     */
    private double maxRequestSize = 50.0;

    /**
     * Counter updated when attachments are added/removed to keep track of total attachments size.
     */
    private double attachedFilesSize;

    private final SimpleStringProperty sizeLimitsText = new SimpleStringProperty();

    /**
     * @param logEntry The log entry template potentially holding a set of attachments. Note
     *                 that files associated with these attachments are considered temporary and
     *                 are subject to removal when the log entry has been committed.
     */
    public AttachmentsEditorController(LogEntry logEntry) {
        this.logEntry = logEntry;
        // If the log entry has an attachment - e.g. log entry created from display or data browser -
        // then add the file size to the total attachments size
        Collection<Attachment> attachments = logEntry.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            attachments.forEach(a -> attachedFilesSize += getFileSize(a.getFile()));
        }
    }

    @FXML
    public void initialize() {

        attachmentsViewController.setAttachments(logEntry.getAttachments());

        filesToDeleteAfterSubmit.addAll(logEntry.getAttachments().stream().map(Attachment::getFile).collect(Collectors.toList()));

        removeButton.setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
        removeButton.disableProperty().bind(Bindings.isEmpty(attachmentsViewController.getSelectedAttachments()));

        attachmentsViewController.addListSelectionChangeListener(change -> {
            // Enable "Embed Selected" button only if exactly one image attachment is selected.
            imageAttachmentSelected.set(attachmentsViewController.getSelectedAttachments().size() == 1 &&
                    attachmentsViewController.getSelectedAttachments().get(0).getContentType().toLowerCase().startsWith("image"));
        });

        embedSelectedButton.disableProperty().bind(imageAttachmentSelected.not());

        sizeLimitsLabel.textProperty().bind(sizeLimitsText);
        sizeLimitsLabel.visibleProperty().bind(Bindings.createBooleanBinding(() -> sizeLimitsText.isNotEmpty().get(), sizeLimitsText));

        sizesErrorLabel.textProperty().bind(sizesErrorMessage);
        sizesErrorLabel.visibleProperty().bind(Bindings.createBooleanBinding(() -> sizesErrorMessage.isNotEmpty().get(), sizesErrorMessage));
    }

    @FXML
    public void addFiles() {
        Platform.runLater(() -> sizesErrorMessage.set(null));
        final FileChooser addFilesDialog = new FileChooser();
        addFilesDialog.setInitialDirectory(new File(System.getProperty("user.home")));
        final List<File> files = addFilesDialog.showOpenMultipleDialog(root.getParent().getScene().getWindow());
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
        Platform.runLater(() -> sizesErrorMessage.set(null));
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
        List<Attachment> attachmentsToRemove =
                new ArrayList<>(attachmentsViewController.getSelectedAttachments());
        attachmentsToRemove.forEach(a -> {
            if (a.getContentType().startsWith("image") && a.getId() != null) { // Null check on id is needed as only embedded image attachments have a non-null id.
                String markup = textArea.getText();
                if (markup != null) {
                    String newMarkup = removeImageMarkup(markup, a.getId());
                    textArea.textProperty().set(newMarkup);
                }
            }
        });
        attachmentsViewController.removeAttachments(attachmentsToRemove);
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
    public void embedSelected() {
        // Just in case... launch dialog only if the first item in the selection is an image
        if (attachmentsViewController.getSelectedAttachments().get(0).getContentType().toLowerCase().startsWith("image")) {
            EmbedImageDialog embedImageDialog = new EmbedImageDialog();
            embedImageDialog.setFile(attachmentsViewController.getSelectedAttachments().get(0).getFile());
            Optional<EmbedImageDescriptor> descriptor = embedImageDialog.showAndWait();
            if (descriptor.isPresent()) {
                String id = UUID.randomUUID().toString();
                attachmentsViewController.getSelectedAttachments().get(0).setId(id);
                addEmbeddedImageMarkup(id, descriptor.get().getWidth(), descriptor.get().getHeight());
            }
        }
    }

    public void deleteTemporaryFiles() {
        JobManager.schedule("Delete temporary attachment files", monitor -> filesToDeleteAfterSubmit.forEach(f -> {
            logger.log(Level.INFO, "Deleting temporary attachment file " + f.getAbsolutePath());
            if (!f.delete()) {
                logger.log(Level.WARNING, "Failed to delete temporary file " + f.getAbsolutePath());
            }
        }));
    }

    private void addEmbeddedImageMarkup(String id, int width, int height) {
        // Insert markup at caret position. At this point an id must be set.
        int caretPosition = textArea.getCaretPosition();
        String imageMarkup =
                "![](attachment/" + id + ")"
                        + "{width=" + width
                        + " height=" + height + "} ";
        textArea.insertText(caretPosition, imageMarkup);
    }

    protected String removeImageMarkup(String markup, String imageId) {
        int index = markup.indexOf(imageId);
        if (index == -1) {
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
     * @param textArea {@link TextArea} for log entry body text.
     */
    public void setTextArea(TextArea textArea) {
        this.textArea = textArea;
    }

    private void addFiles(List<File> files) {
        Platform.runLater(() -> sizesErrorMessage.set(null));
        if (!checkFileSizes(files)) {
            return;
        }
        MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
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
            attachmentsViewController.addAttachment(ologAttachment);
        }
    }

    private void addImage(Image image) {
        addImage(image, UUID.randomUUID().toString());
    }

    private void addImage(Image image, String id) {
        Platform.runLater(() -> sizesErrorMessage.set(null));
        try {
            File imageFile = new File(System.getProperty("java.io.tmpdir"), id + ".png");
            imageFile.deleteOnExit();
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", imageFile);
            double fileSize = getFileSize(imageFile);
            if (fileSize > maxFileSize) {
                showFileSizeExceedsLimit(imageFile);
                return;
            }
            if (attachedFilesSize + fileSize > maxRequestSize) {
                showTotalSizeExceedsLimit();
                return;
            }
            OlogAttachment ologAttachment = new OlogAttachment(id);
            ologAttachment.setContentType("image");
            ologAttachment.setFile(imageFile);
            ologAttachment.setFileName(imageFile.getName());
            attachmentsViewController.addAttachment(ologAttachment);
            filesToDeleteAfterSubmit.add(ologAttachment.getFile());
        } catch (IOException e) {
            Logger.getLogger(AttachmentsEditorController.class.getName())
                    .log(Level.INFO, "Unable to create temp file from clipboard image or embedded image", e);
        }
    }

    public List<Attachment> getAttachments() {
        return attachmentsViewController.getAttachments();
    }

    public void setSizeLimits(String maxFileSize, String maxRequestSize) {
        this.maxFileSize = Double.parseDouble(maxFileSize);
        this.maxRequestSize = Double.parseDouble(maxRequestSize);
        Platform.runLater(() -> sizeLimitsText.set(MessageFormat.format(Messages.SizeLimitsText, maxFileSize, maxRequestSize)));
    }

    /**
     * Checks if files can be accepted with respect to max file size and max total (request) size.
     *
     * @param files List of files to be checked
     * @return <code>false</code> when first file exceeding the max file size limit is encountered, otherwise <code>true</code>.
     */
    private boolean checkFileSizes(List<File> files) {
        double totalSize = 0;
        for (File file : files) {
            double fileSize = getFileSize(file);
            if (fileSize > maxFileSize) {
                showFileSizeExceedsLimit(file);
                return false;
            }
            totalSize += fileSize;
            if ((attachedFilesSize + totalSize) > maxRequestSize) {
                showTotalSizeExceedsLimit();
                return false;
            }
        }
        attachedFilesSize += totalSize;
        return true;
    }

    private double getFileSize(File file) {
        return 1.0 * file.length() / 1024 / 1024;
    }

    private void showFileSizeExceedsLimit(File file) {
        Platform.runLater(() -> sizesErrorMessage.set(MessageFormat.format(Messages.FileTooLarge, file.getName())));
    }

    private void showTotalSizeExceedsLimit() {
        Platform.runLater(() -> sizesErrorMessage.set(Messages.RequestTooLarge));
    }
}
