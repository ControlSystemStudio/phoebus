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
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.logbook.Attachment;
import org.phoebus.ui.javafx.FilesTab;
import org.phoebus.ui.javafx.ImagesTab;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LogAttachmentsController {

    @FXML
    private SplitPane previewPane;
    @FXML
    private ImageView imagePreview;
    @FXML
    private TextArea textPreview;

    @FXML
    private ListView<Attachment> attachmentListView;

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


    }

    public void setImages(ObservableList<Image> images){

    }

    public void setFiles(ObservableList<File> files){

    }
}
