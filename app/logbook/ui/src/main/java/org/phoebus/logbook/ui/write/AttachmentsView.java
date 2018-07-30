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

import org.phoebus.ui.javafx.FilesTab;
import org.phoebus.ui.javafx.ImagesTab;

import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;

/**
 * Collapsible tab pane view that facilitates adding images and files as attachments to log book entries.
 * @author Evan Smith
 *
 */
public class AttachmentsView extends Accordion
{
    @SuppressWarnings("unused")
    private final LogEntryModel    model;
    private final TabPane          tabPane;
    private final ImagesTab     images;
    private final FilesTab      files;
    private final PropertiesTab properties;

    public AttachmentsView(final Node parent, final LogEntryModel model)
    {
        super();
        this.model = model;
        tabPane    = new TabPane();
        images     = new ImagesTab();
        images.setSnapshotNode(parent.getScene().getRoot());
        files      = new FilesTab();
        properties = new PropertiesTab();

        tabPane.getTabs().addAll(images, files, properties);

        TitledPane tPane = new TitledPane("Attachments", tabPane);

        getPanes().add(tPane);
    }

    public List<Image> getImages()
    {
        return images.getImages();
    }

    public void setImages(final List<Image> images)
    {
        this.images.setImages(images);
    }

    public List<File> getFiles()
    {
        return files.getFiles();
    }

    public void setFiles(final List<File> files)
    {
        this.files.setFiles(files);
    }


}
