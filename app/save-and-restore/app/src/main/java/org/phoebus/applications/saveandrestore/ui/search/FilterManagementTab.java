/*
 * Copyright (C) 2020 European Spallation Source ERIC.
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

package org.phoebus.applications.saveandrestore.ui.search;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.javafx.ImageCache;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tab for the filter management view.
 */
public class FilterManagementTab extends Tab {

    public static final String FILTER_MANAGEMENT_TAB = "FilterManagementTab";

    public FilterManagementTab(SaveAndRestoreController saveAndRestoreController) {

        setId(FILTER_MANAGEMENT_TAB);

        final ResourceBundle bundle = NLS.getMessages(SaveAndRestoreApplication.class);

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(SaveAndRestoreController.class.getResource("search/FilterManagement.fxml"));
        loader.setResources(bundle);

        try {
            setContent(loader.load());
        } catch (IOException e) {
            Logger.getLogger(FilterManagementTab.class.getName())
                    .log(Level.SEVERE, "Unable to load search tab content fxml", e);
            return;
        }

        HBox container = new HBox();
        ImageView imageView = new ImageView(ImageCache.getImage(ImageCache.class, "/icons/save-and-restore/manage-filters.png"));
        imageView.setFitWidth(18);
        imageView.setFitHeight(18);
        Label label = new Label(Messages.manageFilters);
        HBox.setMargin(label, new Insets(1, 0, 0, 5));
        container.getChildren().addAll(imageView, label);

        setGraphic(container);

        //((FilterManagementController) loader.getController()).setSearchAndFilterViewController(saveAndRestoreController);

    }
}
