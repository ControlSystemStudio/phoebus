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

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.olog.ui.write.AttachmentsViewController;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the AttachmentPreview.fxml view. It is designed to be used both
 * by a log entry editor and the read-only log entry details view.
 */
public class AttachmentsPreviewController {

    @FXML
    private SplitPane splitPane;

    @FXML
    private StackPane previewPane;

    @FXML
    private ImageView imagePreview;

    @FXML
    private GridPane noPreviewPane;

    @FXML
    private ListView<Attachment> attachmentListView;

    /**
     * List of attachments selected by user in the preview's {@link ListView}.
     */
    private ObservableList<Attachment> selectedAttachments = FXCollections.observableArrayList();

    private SimpleObjectProperty<Attachment> selectedAttachment = new SimpleObjectProperty();

    /**
     * List of listeners that will be notified when user has selected one or multiple attachments in
     * the {@link ListView}.
     */
    private List<ListChangeListener<Attachment>> listSelectionChangeListeners = new ArrayList<>();

    @FXML
    public void initialize() {

        attachmentListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        attachmentListView.setCellFactory(view -> new AttachmentRow());
        attachmentListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<>() {
            /**
             * Shows preview of selected attachment.
             * @param observable
             * @param oldValue
             * @param newValue
             */
            @Override
            public void changed(ObservableValue<? extends Attachment> observable, Attachment oldValue, Attachment newValue) {
                selectedAttachment.set(newValue);
                showPreview();
            }
        });

        attachmentListView.setOnMouseClicked(me -> {
            if (me.getClickCount() == 2) {
                Attachment attachment = attachmentListView.getSelectionModel()
                        .getSelectedItem();
                if(!attachment.getContentType().startsWith("image")){
                    // If there is no external app configured for the file type, show an error message and return.
                    String fileName = attachment.getFile().getName();
                    String[] parts = fileName.split("\\.");
                    if(parts.length == 1 || !ApplicationService.getExtensionsHandledByExternalApp().contains(parts[parts.length - 1])){
                        ExceptionDetailsErrorDialog.openError(Messages.PreviewOpenErrorTitle, Messages.PreviewOpenErrorBody, null);
                        return;
                    }
                }
                ApplicationLauncherService.openFile(attachment.getFile(),
                        false, null);
            }
        });

        attachmentListView.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<>() {
            /**
             * Notifies listeners of list selection change.
             * @param change
             */
            @Override
            public void onChanged(Change<? extends Attachment> change) {
                selectedAttachments.setAll(change.getList());
                listSelectionChangeListeners.stream().forEach(l -> l.onChanged(change));
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem = new MenuItem(Messages.DownloadSelected);
        menuItem.setOnAction(actionEvent -> downloadSelectedAttachments());
        menuItem.disableProperty().bind(Bindings.createBooleanBinding(() -> selectedAttachments.isEmpty(), selectedAttachments));
        contextMenu.getItems().add(menuItem);
        attachmentListView.setContextMenu(contextMenu);

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
                ApplicationLauncherService.openFile(selectedAttachment.get().getFile(),
                        false, null);
            }
            event.consume();
        });
    }

    public ObservableList<Attachment> getSelectedAttachments() {
        return attachmentListView.getSelectionModel().getSelectedItems();
    }

    /**
     * Sets the list of attachments and installs a listener that selects and subsequently shows the last item
     * in the <code>attachments</code>list.
     *
     * @param attachments List of {@link Attachment}s to show in the preview.
     */
    public void setAttachments(ObservableList<Attachment> attachments) {
        attachmentListView.setItems(attachments);
        attachmentListView.getItems().addListener(new ListChangeListener<>() {
            /**
             * Handles a change in the {@link ListView} such that a newly added item is selected and
             * shown in preview. Note that if multiple attachments are added, this method will be called multiple
             * times, and for each call the current selection is cleared. Consequently the last attachment
             * will end up being selected and shown in preview.
             * @param change
             */
            @Override
            public void onChanged(Change<? extends Attachment> change) {
                while (change.next()) {
                    if (change.wasAdded()) {
                        attachmentListView.getSelectionModel().clearSelection();
                        attachmentListView.getSelectionModel().select(change.getAddedSubList().get(0));
                    }
                }
            }
        });
        // Automatically select first attachment.
        if (attachments != null && attachments.size() > 0) {
            attachmentListView.getSelectionModel().select(attachments.get(0));
        }
    }

    private class AttachmentRow extends ListCell<Attachment> {
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
    private void showPreview() {
        if (selectedAttachment.get() == null) {
            imagePreview.visibleProperty().setValue(false);
            return;
        }
        if (selectedAttachment.get().getContentType().startsWith("image")) {
            showImagePreview(selectedAttachment.get());
        } else {
            imagePreview.visibleProperty().setValue(false);
            noPreviewPane.visibleProperty().setValue(true);
        }
    }

    /**
     * Shows image preview in preview pane. The size of the {@link ImageView} is calculated based on
     * the size of the preview pane and the actual image size such that the complete image is always shown.
     *
     * @param attachment
     */
    private void showImagePreview(Attachment attachment) {
        try {
            BufferedImage bufferedImage = ImageIO.read(attachment.getFile());
            // BufferedImage may be null due to lazy loading strategy.
            if (bufferedImage == null) {
                return;
            }
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
            imagePreview.visibleProperty().setValue(true);
            imagePreview.setImage(image);
        } catch (IOException ex) {
            Logger.getLogger(AttachmentsViewController.class.getName())
                    .log(Level.SEVERE, "Unable to load image file " + attachment.getFile().getAbsolutePath(), ex);
        }
    }

    /**
     * Downloads all selected attachments to folder selected by user.
     */
    public void downloadSelectedAttachments() {
        final DirectoryChooser dialog = new DirectoryChooser();
        dialog.setTitle(Messages.SelectFolder);
        dialog.setInitialDirectory(new File(System.getProperty("user.home")));
        File targetFolder = dialog.showDialog(splitPane.getScene().getWindow());
        JobManager.schedule("Save attachments job", (monitor) ->
        {
            selectedAttachments.stream().forEach(a -> downloadAttachment(targetFolder, a));
        });
    }

    private void downloadAttachment(File targetFolder, Attachment attachment) {
        try {
            File targetFile = new File(targetFolder, attachment.getName());
            if (targetFile.exists()) {
                throw new Exception("Target file " + targetFile.getAbsolutePath() + " exists");
            }
            Files.copy(attachment.getFile().toPath(), targetFile.toPath());
        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(splitPane.getParent(), Messages.FileSave, Messages.FileSaveFailed, e);
        }
    }

    public void addListSelectionChangeListener(ListChangeListener<Attachment> changeListener) {
        listSelectionChangeListeners.add(changeListener);
    }

    public void removeListSelectionChangeListener(ListChangeListener<Attachment> changeListener) {
        listSelectionChangeListeners.remove(changeListener);
    }
}
