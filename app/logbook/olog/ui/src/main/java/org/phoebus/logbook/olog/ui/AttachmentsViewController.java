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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogEntry;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the AttachmentPreview.fxml view. It is designed to be used both
 * by a log entry editor and the read-only log entry details view.
 */
public class AttachmentsViewController {

    @FXML
    @SuppressWarnings("unused")
    private SplitPane splitPane;

    @FXML
    @SuppressWarnings("unused")
    private StackPane previewPane;

    @FXML
    @SuppressWarnings("unused")
    private ImageView imagePreview;

    @FXML
    @SuppressWarnings("unused")
    private GridPane noPreviewPane;

    @FXML
    @SuppressWarnings("unused")
    private ListView<Attachment> attachmentListView;

    @FXML
    @SuppressWarnings("unused")
    private Label placeholderLabel;

    /**
     * List of attachments selected by user in the {@link ListView}.
     */
    private final ObservableList<Attachment> selectedAttachments = FXCollections.observableArrayList();

    /**
     * Keeps track of the {@link Attachment} currently in the {@link ImageView}, if any.
     */
    private final SimpleObjectProperty<Attachment> selectedAttachment = new SimpleObjectProperty<>();

    /**
     * List of all {@link Attachment}s currently in view.
     */
    private final ObservableList<Attachment> attachments = FXCollections.observableArrayList();

    public AttachmentsViewController() {
    }

    @FXML
    public void initialize() {

        attachmentListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        attachmentListView.setCellFactory(view -> new AttachmentRow());

        // Show preview when single selection property changes
        selectedAttachment.addListener((obs, o, n) -> showPreview(n));

        attachmentListView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Attachment>) change -> {
            selectedAttachments.setAll(change.getList());
            if (change.getList().size() == 1) {
                // Set single selection property only if selection contains a single element
                selectedAttachment.set(change.getList().get(0));
            }
            else if(attachments.isEmpty()){
                showPreview(null);
            }
        });

        attachmentListView.setOnMouseClicked(me -> {
            if (me.getClickCount() == 2) {
                Attachment attachment = attachmentListView.getSelectionModel()
                        .getSelectedItem();
                if (!attachment.getContentType().startsWith("image")) {
                    // First try to open the file with an internal Phoebus app
                    AppResourceDescriptor defaultApp = ApplicationLauncherService.findApplication(attachment.getFile().toURI(), true, null);
                    if (defaultApp != null) {
                        defaultApp.create(attachment.getFile().toURI());
                        return;
                    }

                    // If not internal apps are found look for external apps
                    String fileName = attachment.getFile().getName();
                    String[] parts = fileName.split("\\.");
                    if (parts.length == 1 || !ApplicationService.getExtensionsHandledByExternalApp().contains(parts[parts.length - 1])) {
                        // If there is no app configured for the file type, then use the default configured for the OS/User
                        // Note: Do not use Desktop API, as using Java AWT can hang Phoebus / JavaFX Applications
                        try {
                            String filePathString = attachment.getFile().toPath().toUri().toString();
                            PhoebusApplication.INSTANCE.getHostServices().showDocument(filePathString);
                        } catch (Exception e) {
                            ExceptionDetailsErrorDialog.openError(Messages.PreviewOpenErrorTitle, Messages.PreviewOpenErrorBody, null);
                        }
                    }
                } else {
                    showImageAttachment();
                }
            }
        });


        attachmentListView.setOnContextMenuRequested((e) -> {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem menuItem = new MenuItem(Messages.DownloadSelected);
            menuItem.setOnAction(actionEvent -> copySelectedAttachments());
            menuItem.disableProperty().bind(Bindings.createBooleanBinding(selectedAttachments::isEmpty, selectedAttachments));

            contextMenu.getItems().add(menuItem);
            URI selectedResource = !selectedAttachments.isEmpty() ? selectedAttachments.get(0).getFile().toURI() : null;

            if (selectedResource != null) {
                contextMenu.getItems().add(new SeparatorMenuItem());
                final List<AppResourceDescriptor> applications = ApplicationService.getApplications(selectedResource);
                applications.forEach(app -> {
                    MenuItem appMenuItem = new MenuItem(app.getDisplayName());
                    appMenuItem.setGraphic(ImageCache.getImageView(app.getIconURL()));
                    appMenuItem.setOnAction(actionEvent -> showImageAttachment());
                    contextMenu.getItems().add(appMenuItem);
                });
            }
            attachmentListView.setContextMenu(contextMenu);
        });

        imagePreview.fitWidthProperty().bind(previewPane.widthProperty());
        imagePreview.fitHeightProperty().bind(previewPane.heightProperty());
        imagePreview.hoverProperty().addListener((event) -> {
            if (((ReadOnlyBooleanProperty) event).get()) {
                splitPane.getScene().setCursor(Cursor.HAND);
            } else {
                splitPane.getScene().setCursor(Cursor.DEFAULT);
            }
        });

        imagePreview.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (selectedAttachment.get() != null && selectedAttachment.get().getContentType().startsWith("image")) {
                showImageAttachment();
            }
            event.consume();
        });

        attachmentListView.itemsProperty().bindBidirectional(new SimpleListProperty<>(attachments));
    }

    /**
     * Launches the Image Viewer application to show the selected image attachment with a watermark.
     */
    private void showImageAttachment() {
        URI uri = selectedAttachment.get().getFile().toURI();
        URI withWatermark = URI.create(uri + "?watermark=" + LogbookUIPreferences.show_log_watermark);
        ApplicationLauncherService.openResource(withWatermark,
                false, null);
    }

    public ObservableList<Attachment> getSelectedAttachments() {
        return attachmentListView.getSelectionModel().getSelectedItems();
    }

    public void invalidateAttachmentList(LogEntry logEntry) {
        if (logEntry.getAttachments().isEmpty()) {
            placeholderLabel.setText(Messages.NoAttachments);
        } else {
            placeholderLabel.setText(Messages.DownloadingAttachments);
        }
        attachments.setAll(Collections.emptyList());
    }

    public void setAttachments(Collection<Attachment> attachmentsList) {
        Platform.runLater(() -> {
            this.attachments.setAll(attachmentsList);
            // Update UI
            if (!this.attachments.isEmpty()) {
                attachmentListView.getSelectionModel().select(this.attachments.get(0));
            }
        });
    }

    private static class AttachmentRow extends ListCell<Attachment> {
        @Override
        public void updateItem(Attachment attachment, boolean empty) {
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
     */
    private void showPreview(Attachment attachment) {
        if (attachment == null) {
            imagePreview.visibleProperty().setValue(false);
            return;
        }
        if (attachment.getContentType().startsWith("image")) {
            imagePreview.visibleProperty().setValue(true);
            noPreviewPane.visibleProperty().setValue(false);
            showImagePreview(attachment);
        } else {
            imagePreview.visibleProperty().setValue(false);
            noPreviewPane.visibleProperty().setValue(true);
        }
    }

    /**
     * Shows image preview in preview pane. The size of the {@link ImageView} is calculated based on
     * the size of the preview pane and the actual image size such that the complete image is always shown.
     *
     * @param attachment The image {@link Attachment} selected by user.
     */
    private void showImagePreview(Attachment attachment) {
        if (attachment.getFile() != null && attachment.getFile().exists()) {
            // Load image data off UI thread...
            JobManager.schedule("Show image attachment", monitor -> {
                try {
                    BufferedImage bufferedImage = ImageIO.read(attachment.getFile());
                    // BufferedImage may be null due to lazy loading strategy.
                    if (bufferedImage == null) {
                        return;
                    }
                    Platform.runLater(() -> {
                        Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                        imagePreview.visibleProperty().setValue(true);
                        imagePreview.setImage(image);
                    });
                } catch (IOException ex) {
                    Logger.getLogger(AttachmentsViewController.class.getName())
                            .log(Level.SEVERE, "Unable to load image file " + attachment.getFile().getAbsolutePath(), ex);
                }
            });
        }
    }

    /**
     * Copies all selected attachments to folder selected by user. Note that attachment files are
     * downloaded from service as soon as user has selected a log entry in the search result view.
     */
    public void copySelectedAttachments() {
        final DirectoryChooser dialog = new DirectoryChooser();
        dialog.setTitle(Messages.SelectFolder);
        dialog.setInitialDirectory(new File(System.getProperty("user.home")));
        File targetFolder = dialog.showDialog(splitPane.getScene().getWindow());
        JobManager.schedule("Save attachments job", (monitor) ->
                selectedAttachments.forEach(a -> copyAttachment(targetFolder, a)));
    }

    private void copyAttachment(File targetFolder, Attachment attachment) {
        try {
            File targetFile = new File(targetFolder, attachment.getName());
            if (targetFile.exists()) {
                throw new Exception("Target file " + targetFile.getAbsolutePath() + " exists");
            }
            Files.copy(attachment.getFile().toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(splitPane.getParent(), Messages.FileSave, Messages.FileSaveFailed, e);
        }
    }

    /**
     * @return The {@link ObservableList} of attachments.
     */
    public ObservableList<Attachment> getAttachments() {
        return attachments;
    }

    /**
     * Adds {@link Attachment}s to the list of {@link Attachment}s. If only one
     * {@link Attachment} is added it is also selected.
     *
     * @param attachmentsList The new {@link Attachment}s
     */
    public void addAttachments(List<Attachment> attachmentsList) {
        Platform.runLater(() -> {
            attachmentListView.getSelectionModel().clearSelection();
            attachments.addAll(attachmentsList);
            attachmentListView.getSelectionModel().select(attachmentsList.get(0));
        });
    }
}
