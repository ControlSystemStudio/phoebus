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
 */

package org.phoebus.applications.saveandrestore;

import javafx.fxml.FXMLLoader;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.search.SearchToolbarController;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import java.net.URI;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SaveAndRestoreInstance implements AppInstance {

    private final AppDescriptor appDescriptor;
    private SaveAndRestoreController controller;

    public SaveAndRestoreInstance(AppDescriptor appDescriptor, URI uri) {
        this.appDescriptor = appDescriptor;

        DockItem tab = null;

        FXMLLoader loader = new FXMLLoader();
        try {
            ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
            loader.setResources(resourceBundle);
            loader.setLocation(SaveAndRestoreApplication.class.getResource("ui/SaveAndRestoreUI.fxml"));
            loader.setControllerFactory(clazz -> {
                try {
                    if (clazz.isAssignableFrom(SaveAndRestoreController.class)) {
                        return clazz.getConstructor(URI.class).newInstance(uri);
                    }
                    else if(clazz.isAssignableFrom(SearchToolbarController.class)){
                        return clazz.getConstructor().newInstance();
                    }
                } catch (Exception e) {
                    Logger.getLogger(SaveAndRestoreInstance.class.getName()).log(Level.WARNING, "Failed to load Save & Restore UI", e);
                }
                return null;
            });
            tab = new DockItem(this, loader.load());
        } catch (Exception e) {
            Logger.getLogger(SaveAndRestoreApplication.class.getName()).log(Level.SEVERE, "Failed loading fxml", e);
        }

        controller = loader.getController();

        tab.setOnCloseRequest(event -> controller.saveLocalState());

        DockPane.getActiveDockPane().addTab(tab);
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return appDescriptor;
    }

    @Override
    public void save(Memento memento) {
        controller.saveLocalState();
    }

    public void openResource(URI uri) {
        controller.openResource(uri);
    }
}
