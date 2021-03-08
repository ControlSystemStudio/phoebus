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

package org.phoebus.logbook.olog.ui;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import org.phoebus.ui.javafx.FilesTab;
import org.phoebus.ui.javafx.ImagesTab;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LogAttachmentsController {

    @FXML
    private TabPane tabPane;

    private ImagesTab imagesTab;
    private FilesTab filesTab;

    private List<Image> images;
    private List<File> files;

    public LogAttachmentsController(List<Image> images, List<File> files){
        this.images = images;
        this.files = files;
    }

    public LogAttachmentsController(){
        this.images = new ArrayList<>();
        this.files = new ArrayList<>();
    }

    @FXML
    public void initialize(){

        imagesTab = new ImagesTab(false);
        imagesTab.setImages(images);

        filesTab = new FilesTab(false);
        filesTab.setFiles(files);

        tabPane.getTabs().add(0, imagesTab);
        tabPane.getTabs().add(1, filesTab);

        tabPane.getSelectionModel().selectFirst();

    }

    public void setImages(ObservableList<Image> images){
        imagesTab.setImages(images);
    }

    public void setFiles(ObservableList<File> files){
        filesTab.setFiles(files);
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
