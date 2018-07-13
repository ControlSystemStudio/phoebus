/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.phoebus.framework.jobs.JobManager;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Tab View for adding files as attachments to a log entry.
 * @author Evan Smith
 *
 */
public class FilesTab extends Tab
{
    private class FileCell extends ListCell<File>
    {
        private Hyperlink hyperlink;
        private File file;
        
        public FileCell()
        {
            super();
            hyperlink = new Hyperlink();
            hyperlink.setOnAction(event -> 
            {
                JobManager.schedule("Open Attached File", monitor ->
                {
                    // Open the file in the default editor.
                    if (Desktop.isDesktopSupported())
                    {
                        Desktop desktop = Desktop.getDesktop();
                        try
                        {
                            desktop.open(file);
                        } 
                        catch (IOException ex)
                        {
                            logger.log(Level.WARNING, "Could not open file in default editor.", ex);
                        }
                    }
                });
            });
        }
        
        @Override
        public void updateItem(File file, boolean empty)
        {
            super.updateItem(file, empty);
            if (empty)
            {
                setGraphic(null);
            }
            else
            {
                this.file = file;
                hyperlink.setText(file.getName());
                setGraphic(hyperlink);
            }   
        }
    }
    
    private final LogEntryModel  model;
    
    private final VBox           content;
    private final Label          label;
    private final ListView<File> listView;
    private final HBox           listBox, buttonBox;
    private final Button         attachContext, attachFile, removeSelected;
    
    private final FileChooser    addFileDialog;  
    
    public FilesTab(final LogEntryModel model) 
    {
        super();
        this.model = model;
        
        content   = new VBox();
        label     = new Label("Attached Files");
        listView  = new ListView<File>(model.getFiles());
        listBox   = new HBox();
        buttonBox = new HBox();
        attachContext  = new Button("Attach Context");
        attachFile     = new Button("Attach File");
        removeSelected = new Button("Remove Selected");
        
        addFileDialog = new FileChooser();
        
        formatTab();
    }

    private void formatTab()
    {
        setText("Files");
        setClosable(false);
        setTooltip(new Tooltip("Add files to log entry."));
        
        addFileDialog.setInitialDirectory(new File(System.getProperty("user.home")));
        
        formatContent();
        setOnActions();

        setContent(content);
    }

    private void formatContent()
    {
        VBox.setMargin(label, new Insets(10, 0, 0, 10));
        
        formatListBox();
        formatButtonBox();
        
        content.setSpacing(10);
        content.getChildren().addAll(label, listBox, buttonBox);
    }

    
    private void formatListBox()
    {
        listView.setCellFactory(cell -> new FileCell());
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setPrefHeight(50);
        // Background of cells is same as default view background.
        listView.setStyle("-fx-control-inner-background-alt: #f4f4f4");
        listView.setStyle("-fx-control-inner-background: #f4f4f4");
        VBox.setMargin(listBox, new Insets(0, 10, 0, 10));
        VBox.setVgrow(listBox, Priority.ALWAYS);
        HBox.setHgrow(listView, Priority.ALWAYS);
        listBox.getChildren().add(listView);
    }

    private void formatButtonBox()
    {
        // TODO Add an addContext tool tip when addContext is implemented.
        attachFile.setTooltip(new Tooltip("Attach a file to the log entry."));
        removeSelected.setTooltip(new Tooltip("Remove the selected file(s)."));
        
        buttonBox.setSpacing(10);
        VBox.setMargin(buttonBox, new Insets(0, 10, 10, 10));
        
        attachContext.prefWidthProperty().bind(buttonBox.widthProperty().divide(3));
        attachFile.prefWidthProperty().bind(buttonBox.widthProperty().divide(3));
        removeSelected.prefWidthProperty().bind(buttonBox.widthProperty().divide(3));

        buttonBox.getChildren().addAll(attachContext, attachFile, removeSelected);
    }
    
    private void setOnActions()
    {
        // TODO : Implement attach context
        
        attachFile.setOnAction(event -> 
        {
            Window ownerWindow = this.getTabPane().getParent().getScene().getWindow();
            List<File> files = addFileDialog.showOpenMultipleDialog(ownerWindow);
            if (null != files)
            {
                for (File file : files)
                {
                    model.addFile(file);
                }
            }
        });
        
        removeSelected.setOnAction(event ->
        {
            // We can't alter a list that we are iterating over, so we iterate over a copy of the selected files list.
            List<File> files = List.copyOf(listView.getSelectionModel().getSelectedItems());
            for (File file : files)
            {
                model.removeFile(file);
            }
        });
    }
}
