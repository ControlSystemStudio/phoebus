/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.saveandrestore.ui.saveset;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.phoebus.applications.saveandrestore.SpringFxmlLoader;
import org.phoebus.applications.saveandrestore.data.NodeChangedListener;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.applications.saveandrestore.model.Node;

public class SaveSetTab extends Tab implements NodeChangedListener{

    private SaveSetController saveSetController;

    private SaveAndRestoreService saveAndRestoreService;

    private SimpleStringProperty tabTitleProperty = new SimpleStringProperty();


    public SaveSetTab(Node node, SaveAndRestoreService saveAndRestoreService){

        this.saveAndRestoreService = saveAndRestoreService;

        setId(node.getUniqueId());

        SpringFxmlLoader springFxmlLoader = new SpringFxmlLoader();
        try {
            setContent((javafx.scene.Node)springFxmlLoader.load("ui/saveset/SaveSetEditor.fxml"));
            saveSetController = springFxmlLoader.getLoader().getController();
            tabTitleProperty.set(node.getName());
            setGraphic(getTabGraphic());
            saveSetController.loadSaveSet(node);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        setOnCloseRequest(event -> {
            if(!saveSetController.handleSaveSetTabClosed()){
                event.consume();
            }
            else{
                saveAndRestoreService.removeNodeChangeListener(this);
            }
        });

        saveAndRestoreService.addNodeChangeListener(this);
    }

    private javafx.scene.Node getTabGraphic(){
        HBox container = new HBox();
        Image icon = ImageCache.getImage(SnapshotTab.class, "/icons/save-and-restore/saveset.png");
        ImageView imageView = new ImageView(icon);
        Label label = new Label("");
        label.textProperty().bindBidirectional(tabTitleProperty);
        HBox.setMargin(label, new Insets(3, 5, 0,5));
        container.getChildren().addAll(imageView, label);

        return container;
    }

    @Override
    public void nodeChanged(Node node){
        if(node.getUniqueId().equals(getId())) {
            tabTitleProperty.set(node.getName());
        }
    }
}
