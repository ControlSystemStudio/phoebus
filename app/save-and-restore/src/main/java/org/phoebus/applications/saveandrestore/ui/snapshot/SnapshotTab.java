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

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.phoebus.applications.saveandrestore.SpringFxmlLoader;
import org.phoebus.applications.saveandrestore.data.NodeChangeListener;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.saveset.SaveSetTab;
import org.phoebus.ui.javafx.ImageCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

public class SnapshotTab extends Tab implements TabTitleChangedListener, NodeChangeListener {

    public SaveAndRestoreService saveAndRestoreService;

    private SimpleStringProperty tabTitleProperty = new SimpleStringProperty();

    private SnapshotController snapshotController;

    private SimpleObjectProperty<Image> tabGraphicImageProperty = new SimpleObjectProperty<Image>();

    private Image regularImage;
    private Image goldenImage;

    private static Executor UI_EXECUTOR = Platform::runLater;

    public SnapshotTab(se.esss.ics.masar.model.Node node, SaveAndRestoreService saveAndRestoreService){

        this.saveAndRestoreService = saveAndRestoreService;

        setId(node.getUniqueId());

        SpringFxmlLoader springFxmlLoader = new SpringFxmlLoader();
        try {

            VBox borderPane = (VBox)springFxmlLoader.load("/org/phoebus/applications/saveandrestore/ui/snapshot/fxml/SnapshotEditor.fxml");
            setContent(borderPane);

            regularImage = ImageCache.getImage(SnapshotTab.class, "/icons/small/Snap-shot@.png");
            goldenImage = ImageCache.getImage(SnapshotTab.class, "/icons/small/Snap-shot-golden@.png");

            HBox container = new HBox();
            ImageView imageView = new ImageView();
            imageView.imageProperty().bind(tabGraphicImageProperty);
            Label label = new Label("");
            label.textProperty().bind(tabTitleProperty);
            HBox.setMargin(label, new Insets(1, 0, 0,5));
            container.getChildren().addAll(imageView, label);

            setGraphic(container);

            snapshotController = springFxmlLoader.getLoader().getController();
            snapshotController.setTabTitleChangedListener(this);

            tabGraphicImageProperty.set(Boolean.parseBoolean(node.getProperty("golden")) ? goldenImage : regularImage);

            saveAndRestoreService.addNodeChangeListener(this);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        setOnCloseRequest(event -> {
            if(!snapshotController.handleSnapshotTabClosed()){
                event.consume();
            }
            else{
                saveAndRestoreService.removeNodeChangeListener(this);
            }
        });
    }

    public void loadSnapshot(se.esss.ics.masar.model.Node node){
        snapshotController.setTabTitleChangedListener(this);
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

    @Override
    public void tabTitleChanged(String tabTitle){
        tabTitleProperty.set(tabTitle);
    }

    @Override
    public void nodeChanged(se.esss.ics.masar.model.Node node){

        UI_EXECUTOR.execute(() -> {
            tabGraphicImageProperty.set(Boolean.parseBoolean(node.getProperty("golden")) ? goldenImage : regularImage);
            tabTitleProperty.set(node.getName());
        });
    }
}
