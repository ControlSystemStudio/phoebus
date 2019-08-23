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
import org.phoebus.applications.saveandrestore.SpringFxmlLoader;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.ui.javafx.ImageCache;
import se.esss.ics.masar.model.Node;

public class SnapshotTab extends Tab {

    public SaveAndRestoreService saveAndRestoreService;

    private SimpleStringProperty tabTitleProperty = new SimpleStringProperty();

    private SnapshotController snapshotController;

    private SimpleObjectProperty<Image> tabGraphicImageProperty = new SimpleObjectProperty<Image>();

    private Image regularImage;
    private Image goldenImage;

    private SimpleObjectProperty<Node> nodeSimpleObjectProperty;

    public SnapshotTab(se.esss.ics.masar.model.Node node, SaveAndRestoreService saveAndRestoreService){

        this.saveAndRestoreService = saveAndRestoreService;

        setId(node.getUniqueId());

        SpringFxmlLoader springFxmlLoader = new SpringFxmlLoader();
        try {

            VBox borderPane = (VBox)springFxmlLoader.load("/org/phoebus/applications/saveandrestore/ui/snapshot/fxml/SnapshotEditor.fxml");
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
            nodeSimpleObjectProperty = snapshotController.getNodeSimpleObjectProperty();

            tabGraphicImageProperty.set(Boolean.parseBoolean(node.getProperty("golden")) ? goldenImage : regularImage);

            nodeSimpleObjectProperty.addListener((observable, oldValue, newValue) -> {
                tabGraphicImageProperty.set(Boolean.parseBoolean(newValue.getProperty("golden")) ? goldenImage : regularImage);
                tabTitleProperty.set(newValue.getName());
            });

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        setOnCloseRequest(event -> {
            if(!snapshotController.handleSnapshotTabClosed()){
                event.consume();
            }
        });
    }

    public void loadSnapshot(se.esss.ics.masar.model.Node node){
        snapshotController.loadSnapshot(node);
    }

    public void loadSaveSet(se.esss.ics.masar.model.Node node){
        snapshotController.loadSaveSet(node);
    }

    public void addSnapshot(se.esss.ics.masar.model.Node node){
        snapshotController.addSnapshot(node);
    }

}
