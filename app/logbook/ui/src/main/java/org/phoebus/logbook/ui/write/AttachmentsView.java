/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import javafx.scene.control.Accordion;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;

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
    
    public AttachmentsView(final LogEntryModel model)
    {
        super();
        this.model = model;
        tabPane    = new TabPane();
        images     = new ImagesTab(model);
        files      = new FilesTab(model);
        properties = new PropertiesTab();
        
        tabPane.getTabs().addAll(images, files, properties);
        
        TitledPane tPane = new TitledPane("Attachments", tabPane);
        
        getPanes().add(tPane);
    }
}
