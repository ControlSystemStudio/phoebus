/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import java.io.File;
import java.util.List;

import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Screenshot;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
/**
 * @author Evan Smith
 */
public class ImagesTab extends Tab
{
    private class ImageCell extends ListCell<Image>
    {
        private ImageView cellImageView = new ImageView();
         
        public ImageCell(final ImageView imageView)
        {
            super();
            setAlignment(Pos.CENTER);
            cellImageView.fitWidthProperty().bind(imageList.widthProperty().subtract(50));
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
    
    private static final ImageView removeIcon = ImageCache.getImageView(ImageCache.class, "/icons/delete.png");
    
    private final LogEntryModel model;
    
    private final VBox        content, removeBox, listBox;
    private final HBox        imageBox, imageViewBox;
    private final ImageView   imageView;
    private final HBox        buttonBox;
    private final Button      addImage, captureWindow, clipboard, removeImage;
    private final FileChooser addImageDialog;
    private final ListView<Image>  imageList;

    public ImagesTab(final LogEntryModel model)
    {
        super();
        this.model = model;
        
        content       = new VBox();
        imageBox      = new HBox();
        imageViewBox  = new HBox();
        imageView     = new ImageView();
        removeBox     = new VBox();
        removeImage   = new Button("Remove", removeIcon);
        listBox       = new VBox();
        imageList     = new ListView<Image>(model.getImages());        
        buttonBox     = new HBox();
        addImage      = new Button("Add Image");
        captureWindow     = new Button("CSS Window");
        clipboard     = new Button("Clipboard Image");
        
        addImageDialog = new FileChooser();
        
        formatTab();
    }

    private void formatTab()
    {
        setText("Images");
        setClosable(false);
        setTooltip(new Tooltip("Add images to log entry."));

        addImageDialog.setInitialDirectory(new File(System.getProperty("user.home")));
        addImageDialog.getExtensionFilters().addAll(
                new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.ppm" , "*.pgm"));

        formatContent();
        setOnActions(); 
    }
    
    /** Format the tab content */
    private void formatContent()
    {
        formatImageBox();
        formatButtonBox();
        
        content.setSpacing(10);
        VBox.setMargin(imageBox,  new Insets(10, 0, 0, 0));
        VBox.setMargin(buttonBox, new Insets(0, 0, 10, 0));
        content.getChildren().addAll(imageBox, buttonBox);
        
        this.setContent(content);
    }

    private void formatImageBox()
    {
        imageBox.setPrefHeight(250);
        
        imageBox.setSpacing(10);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.getChildren().addAll(imageViewBox, removeBox, listBox);
        
        VBox.setMargin(imageBox, new Insets(0, 0, 0, 10));
        
        formatImageViewBox();
        formatRemoveBox();
        formatListBox();
    }
    
    private void formatImageViewBox()
    {
        imageView.fitHeightProperty().bind(imageBox.heightProperty());
        imageView.setPreserveRatio(true);
        imageView.imageProperty().bind(imageList.getSelectionModel().selectedItemProperty());
        HBox.setHgrow(imageViewBox, Priority.ALWAYS);
        imageViewBox.prefWidthProperty().bind(imageBox.widthProperty().divide(3));
        imageViewBox.setAlignment(Pos.CENTER);
        imageViewBox.getChildren().add(imageView);
        HBox.setMargin(imageViewBox, new Insets(0, 0, 0, 10));
    }
    
    private void formatRemoveBox()
    {
        removeImage.setTooltip(new Tooltip("Remove the selected image."));
        removeBox.setMinWidth(110);
        removeImage.setPrefSize(100, 30);
        removeBox.getChildren().add(removeImage);
    }
    
    private void formatListBox()
    {
        imageList.setStyle("-fx-control-inner-background-alt: #f4f4f4");
        imageList.setStyle("-fx-control-inner-background: #f4f4f4");

        imageList.setCellFactory(param -> new ImageCell(imageView));
        imageList.prefWidthProperty().bind(imageBox.widthProperty().divide(5));
        listBox.setSpacing(5);
        HBox.setMargin(listBox, new Insets(0, 10, 0, 0));
        listBox.setAlignment(Pos.CENTER_LEFT);
        listBox.getChildren().addAll(new Label("Images: "), imageList);
    }
    
    private void formatButtonBox()
    {
        addImage.setTooltip(new Tooltip("Add an image to the log entry."));
        captureWindow.setTooltip(new Tooltip("Add a capture of the application window to the log entry."));
        clipboard.setTooltip(new Tooltip("Add an image from the clipboard to the log entry."));

        buttonBox.setSpacing(10);

        HBox.setMargin(addImage,  new Insets(0,  0, 0, 10));
        HBox.setMargin(clipboard, new Insets(0, 10, 0,  0));
        
        buttonBox.setAlignment(Pos.CENTER);
        
        // Give each button 1/4 of the room.
        addImage.prefWidthProperty().bind(buttonBox.widthProperty().divide(3));
        captureWindow.prefWidthProperty().bind(buttonBox.widthProperty().divide(3));
        clipboard.prefWidthProperty().bind(buttonBox.widthProperty().divide(3));
        
        buttonBox.getChildren().addAll(addImage, captureWindow, clipboard);
    }
    
    private void setOnActions()
    {
        
        model.addImagesListener(new ListChangeListener<Image>()
        {
            @Override
            public void onChanged(Change<? extends Image> c)
            {
                if (c.next())
                    if (c.wasAdded())
                        imageList.getSelectionModel().selectLast();
            }
        });
        
        addImage.setOnAction(event ->
        {
            List<File> imageFiles = addImageDialog.showOpenMultipleDialog(getTabPane().getScene().getWindow());
            if (null != imageFiles)
            {
                for (File imageFile : imageFiles)
                {
                    Image image = new Image(imageFile.toURI().toString());
                    model.addImage(image);
                }
            }
        });
        
        removeImage.setOnAction(event ->
        {
            Image image = imageView.getImage();
            if (image != null)
            {
                model.removeImage(imageView.getImage());
            }
        });

        captureWindow.setOnAction(event ->
        {
            model.addImage(Screenshot.imageFromNode(model.getScene().getRoot()));
        });
        
        clipboard.setOnAction(event -> 
        {
            Image image = Screenshot.getImageFromClipboard();
            model.addImage(image);
        });
    }  
}
