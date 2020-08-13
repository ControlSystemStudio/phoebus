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

package org.phoebus.logbook.ui.write;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import org.phoebus.logbook.ui.Messages;
import org.phoebus.ui.javafx.FilesTab;
import org.phoebus.ui.javafx.ImagesTab;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AttachmentsViewController {

    @FXML
    private TitledPane titledPane;

    @FXML
    private TabPane tabPane;

    private ImagesTab imagesTab;
    private FilesTab filesTab;
    private Node parent;
    private List<Image> images;
    private List<File> files;
    private boolean autoExpand;

    public AttachmentsViewController(Node parent, List<Image> images, List<File> files, Boolean autoExpand){
        this.parent = parent;
        this.images = images;
        this.files = files;
        this.autoExpand = autoExpand;
    }

    public AttachmentsViewController(Node parent, Boolean autoExpand){
        this.parent = parent;
        this.autoExpand = autoExpand;
        this.images = new ArrayList<>();
        this.files = new ArrayList<>();
    }

    @FXML
    public void initialize(){

        localize();

        imagesTab = new ImagesTab();
        imagesTab.setSnapshotNode(parent.getScene().getRoot());
        imagesTab.setImages(images);

        filesTab = new FilesTab();
        filesTab.setFiles(files);

        tabPane.getTabs().add(0, imagesTab);
        tabPane.getTabs().add(1, filesTab);

        tabPane.getSelectionModel().selectFirst();

        // Open/close the attachments pane if there's something to see resp. not
        if(autoExpand && (!this.images.isEmpty() || !this.files.isEmpty())){
            Platform.runLater(() ->
            {
                titledPane.setExpanded(true);
            });
        }
    }

    public void setImages(ObservableList<Image> images){
        imagesTab.setImages(images);
        if(autoExpand && !images.isEmpty()) {
            Platform.runLater(() ->
            {
                titledPane.setExpanded(true);
            });
        }
    }

    public void setFiles(ObservableList<File> files){
        filesTab.setFiles(files);
        if(autoExpand && !files.isEmpty()) {
            Platform.runLater(() ->
            {
                titledPane.setExpanded(true);
            });
        }
    }

    private void localize(){
        titledPane.setText(Messages.Attachments);
    }

    public List<Image> getImages()
    {
        return imagesTab.getImages();
    }

    public List<File> getFiles()
    {
        return filesTab.getFiles();
    }

}
