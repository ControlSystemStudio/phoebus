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
package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.phoebus.ui.javafx.ImageCache;


public class SnapshotTab extends Tab implements TabTitleChangedListener{

    private SimpleStringProperty tabTitleProperty = new SimpleStringProperty();

    private SnapshotController snapshotController;

    public SnapshotTab(se.esss.ics.masar.model.Node node){

        //tabTitleProperty.set(node.getName() + " (" + node.getCreated() + ")");
        setId(node.getUniqueId());

        FXMLLoader loader = new FXMLLoader();
        try {
            loader.setLocation(this.getClass().getResource("fxml/SnapshotEditor.fxml"));

            VBox borderPane = loader.load();
            setContent(borderPane);
            setGraphic(getTabGraphic());

            snapshotController = loader.getController();
            snapshotController.setTabTitleChangedListener(this);


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        setOnCloseRequest(event -> {
            if(snapshotController.handleSnapshotTabClosed()){
                event.consume();
            }
        });
    }

    public void loadSnapshot(se.esss.ics.masar.model.Node node){
        Task task = new Task<Void>(){
            @Override
            public Void call() {
                snapshotController.loadSnapshot(node);
                return null;
            }
        };

        new Thread(task).start();
    }

    public void loadSaveSet(se.esss.ics.masar.model.Node node){
        Task task = new Task<Void>(){
            @Override
            public Void call() {
                tabTitleProperty.set("<unnamed snapshot>");
                snapshotController.loadSaveSet(node.getUniqueId());
                return null;
            }
        };

        new Thread(task).start();
    }

    public void addSnapshot(se.esss.ics.masar.model.Node node){
        snapshotController.addSnapshot(node);
    }


    private Node getTabGraphic(){
        HBox container = new HBox();
        Image icon = ImageCache.getImage(SnapshotTab.class, "/icons/ksnapshot.png");
        ImageView imageView = new ImageView(icon);
        Label label = new Label("");
        label.textProperty().bind(tabTitleProperty);
        HBox.setMargin(label, new Insets(0, 5, 0,5));
        container.getChildren().addAll(imageView, label);

        return container;
    }

    @Override
    public void tabTitleChanged(String tabTitle){
        tabTitleProperty.set(tabTitle);
    }
}
