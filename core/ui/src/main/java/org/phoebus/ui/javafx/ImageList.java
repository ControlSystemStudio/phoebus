/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import java.io.File;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/** List of images, with buttons to add/remove screen shot, image from file system or clip board.
 *  @author Evan Smith
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageList extends VBox
{
    private class ImageCell extends ListCell<Image>
    {
        private ImageView cellImageView = new ImageView();

        public ImageCell(final ImageView imageView)
        {
            setAlignment(Pos.CENTER);
            cellImageView.fitWidthProperty().bind(images.widthProperty().subtract(50));
            cellImageView.setPreserveRatio(true);
        }

        @Override
        public void updateItem(Image image, boolean empty)
        {
            super.updateItem(image, empty);
            if (empty)
                setGraphic(null);
            else
            {
                cellImageView.setImage(image);
                setGraphic(cellImageView);
            }
        }
    }

    private final ImageView preview = new ImageView();
    private final ListView<Image> images = new ListView<>();
    private Node snapshot_node;

    /** @param root_node Node that will be used to obtain a screenshot */
    public ImageList()
    {
        final Node images = createImageSection();
        final Node buttons = createButtons();
        VBox.setVgrow(images, Priority.ALWAYS);

        setSpacing(5);
        getChildren().setAll(images, buttons);
        setPadding(new Insets(5));
    }

    /** @param node Node to use when taking a screenshot */
    public void setSnapshotNode(final Node node)
    {
        snapshot_node = node;
    }

    /** @param images Images to show */
    public void setImages(final List<Image> images)
    {
        this.images.getItems().setAll(images);
        selectFirstImage();
    }

    /** @return Images shown in the tab */
    public List<Image> getImages()
    {
        return images.getItems();
    }

    private Node createImageSection()
    {
        preview.setPreserveRatio(true);
        preview.setManaged(false);

        final Button removeImage   = new Button(Messages.Remove, ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
        removeImage.setTooltip(new Tooltip(Messages.RemoveImage));
        removeImage.setOnAction(event ->
        {
            final Image image = preview.getImage();
            if (image != null)
            {
                images.getItems().remove(image);
                selectFirstImage();
            }
        });

        final StackPane left = new StackPane(preview, removeImage);
        // Image in background fills the area
        preview.setX(5);
        preview.setY(5);
        preview.fitWidthProperty().bind(left.widthProperty());
        preview.fitHeightProperty().bind(left.heightProperty());
        // Remove button on top, upper right corner
        StackPane.setAlignment(removeImage, Pos.TOP_RIGHT);
        StackPane.setMargin(removeImage, new Insets(5));

        images.setPlaceholder(new Label(Messages.NoImages));
        images.setStyle("-fx-control-inner-background-alt: #f4f4f4");
        images.setStyle("-fx-control-inner-background: #f4f4f4");
        images.setCellFactory(param -> new ImageCell(preview));

        // Show selected image in preview
        preview.imageProperty().bind(images.getSelectionModel().selectedItemProperty());
        // Enable button if something is selected
        removeImage.disableProperty().bind(Bindings.isEmpty(images.getSelectionModel().getSelectedItems()));

        VBox.setVgrow(images, Priority.ALWAYS);
        final VBox right = new VBox(new Label(Messages.ImagesTitle), images);
        right.setPadding(new Insets(5));

        final SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.7);
        return split;
    }

    private Node createButtons()
    {
        final Button addImage      = new Button(Messages.AddImage);
        final Button captureWindow = new Button(Messages.CSSWindow);
        final Button clipboard     = new Button(Messages.Clipboard);

        addImage.setTooltip(new Tooltip(Messages.AddImageTooltip));
        captureWindow.setTooltip(new Tooltip(Messages.CSSWindowTooltip));
        clipboard.setTooltip(new Tooltip(Messages.ClipboardTooltip));

        addImage.setOnAction(event ->
        {
            final FileChooser addImageDialog = new FileChooser();
            addImageDialog.setInitialDirectory(new File(System.getProperty("user.home")));
            addImageDialog.getExtensionFilters().addAll(
                    new ExtensionFilter(Messages.ImageFiles, "*.png", "*.jpg", "*.ppm" , "*.pgm"));
            final List<File> imageFiles = addImageDialog.showOpenMultipleDialog(getScene().getWindow());
            if (null != imageFiles)
                for (File imageFile : imageFiles)
                {
                    Image image = new Image(imageFile.toURI().toString());
                    images.getItems().add(image);
                }
            selectFirstImage();
        });

        captureWindow.setOnAction(event ->
        {
            images.getItems().add(Screenshot.imageFromNode(snapshot_node));
            selectFirstImage();
        });

        clipboard.setOnAction(event ->
        {
            final Image image = Screenshot.getImageFromClipboard();
            if (null != image)
            {
                images.getItems().add(image);
                selectFirstImage();
            }
        });

        final HBox row = new HBox(10, addImage, captureWindow, clipboard);
        // Have buttons equally split the available width
        addImage.setMaxWidth(Double.MAX_VALUE);
        captureWindow.setMaxWidth(Double.MAX_VALUE);
        clipboard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addImage, Priority.ALWAYS);
        HBox.setHgrow(captureWindow, Priority.ALWAYS);
        HBox.setHgrow(clipboard, Priority.ALWAYS);

        return row;
    }

    private void selectFirstImage()
    {
        if (images.getSelectionModel().isEmpty()  &&  ! images.getItems().isEmpty())
            images.getSelectionModel().select(images.getItems().get(0));
    }
}
