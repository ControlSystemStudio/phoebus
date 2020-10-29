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

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SpringFxmlLoader;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.applications.saveandrestore.model.NodeType;

public class SnapshotTab extends Tab {

    public SaveAndRestoreService saveAndRestoreService;

    private SimpleStringProperty tabTitleProperty = new SimpleStringProperty();

    private SnapshotController snapshotController;

    private SimpleObjectProperty<Image> tabGraphicImageProperty = new SimpleObjectProperty<Image>();

    private Image regularImage;
    private Image goldenImage;


    public SnapshotTab(org.phoebus.applications.saveandrestore.model.Node node, SaveAndRestoreService saveAndRestoreService){

        this.saveAndRestoreService = saveAndRestoreService;

        if(node.getNodeType().equals(NodeType.SNAPSHOT)) {
            setId(node.getUniqueId());
        }

        SpringFxmlLoader springFxmlLoader = new SpringFxmlLoader();

        VBox borderPane = (VBox)springFxmlLoader.load("ui/snapshot/SnapshotEditor.fxml");
        setContent(borderPane);

        regularImage = ImageCache.getImage(SnapshotTab.class, "/icons/save-and-restore/snapshot.png");
        goldenImage = ImageCache.getImage(SnapshotTab.class, "/icons/save-and-restore/snapshot-golden.png");

        HBox container = new HBox();
        ImageView imageView = new ImageView();
        imageView.imageProperty().bind(tabGraphicImageProperty);
        Label label = new Label("");
        label.textProperty().bind(tabTitleProperty);
        HBox.setMargin(label, new Insets(1, 0, 0,5));
        container.getChildren().addAll(imageView, label);

        setGraphic(container);

        snapshotController = springFxmlLoader.getLoader().getController();
        snapshotController.setSnapshotTab(this);
        tabTitleProperty.set(node.getNodeType().equals(NodeType.SNAPSHOT) ? node.getName() : Messages.unnamedSnapshot);

        tabGraphicImageProperty.set(Boolean.parseBoolean(node.getProperty("golden")) ? goldenImage : regularImage);

        setOnCloseRequest(event -> {
            if(!snapshotController.handleSnapshotTabClosed()){
                event.consume();
            }
        });
    }

    public void updateTabTitile(String name, boolean golden){
        tabGraphicImageProperty.set(golden ? goldenImage : regularImage);
        tabTitleProperty.set(name);
    }

    public void loadSnapshot(org.phoebus.applications.saveandrestore.model.Node node){
        snapshotController.loadSnapshot(node);
    }

    public void loadSaveSet(org.phoebus.applications.saveandrestore.model.Node node){
        snapshotController.loadSaveSet(node);
    }

    public void addSnapshot(org.phoebus.applications.saveandrestore.model.Node node){
        snapshotController.addSnapshot(node);
    }
}
