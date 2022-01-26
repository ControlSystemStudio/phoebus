/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.saveandrestore.ui.saveset;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.phoebus.applications.saveandrestore.ui.NodeChangedListener;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.ui.javafx.ImageCache;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SaveSetTab extends Tab implements NodeChangedListener {

    private SaveSetController saveSetController;

    private SaveAndRestoreService saveAndRestoreService;

    private SimpleStringProperty tabTitleProperty = new SimpleStringProperty();


    public SaveSetTab(Node node, SaveAndRestoreService saveAndRestoreService) {

        this.saveAndRestoreService = saveAndRestoreService;

        setId(node.getUniqueId());

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(SaveSetTab.class.getResource("SaveSetEditor.fxml"));
            setContent(loader.load());
            saveSetController = loader.getController();
            tabTitleProperty.set(node.getName());
            setGraphic(getTabGraphic());
            saveSetController.loadSaveSet(node);
        } catch (Exception e) {
            Logger.getLogger(SaveSetTab.class.getName())
                    .log(Level.SEVERE, "Failed to load fxml", e);
            return;
        }

        setOnCloseRequest(event -> {
            if (!saveSetController.handleSaveSetTabClosed()) {
                event.consume();
            } else {
                saveAndRestoreService.removeNodeChangeListener(this);
            }
        });

        saveAndRestoreService.addNodeChangeListener(this);
    }

    private javafx.scene.Node getTabGraphic() {
        HBox container = new HBox();
        Image icon = ImageCache.getImage(SnapshotTab.class, "/icons/save-and-restore/saveset.png");
        ImageView imageView = new ImageView(icon);
        Label label = new Label("");
        label.textProperty().bindBidirectional(tabTitleProperty);
        HBox.setMargin(label, new Insets(3, 5, 0, 5));
        container.getChildren().addAll(imageView, label);

        return container;
    }

    @Override
    public void nodeChanged(Node node) {
        if (node.getUniqueId().equals(getId())) {
            tabTitleProperty.set(node.getName());
        }
    }
}
