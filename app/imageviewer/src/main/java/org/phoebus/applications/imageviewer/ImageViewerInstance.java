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

package org.phoebus.applications.imageviewer;

import javafx.fxml.FXMLLoader;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class ImageViewerInstance implements AppInstance {

    public static final String NAME = "Image Viewer";

    public enum ViewMode{
        TAB,
        DIALOG
    };

    private DockItem dockItem;

    private AppDescriptor appDescriptor;

    public ImageViewerInstance(AppDescriptor appDescriptor, URI uri, ViewMode viewMode){
        this.appDescriptor = appDescriptor;

        if(viewMode == ViewMode.TAB){
            final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, uri);
            final ImageViewerInstance instance;
            if (existing != null) {   // Found one, raise it
                instance = existing.getApplication();
                //instance.raise();
            }

            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(this.getClass().getResource("/ImageViewer.fxml"));
            try {
                fxmlLoader.load();
                ImageViewerController imageViewerController = fxmlLoader.getController();
                imageViewerController.setImage(uri.toURL());
                dockItem = new DockItemWithInput(this, imageViewerController.getRoot(), uri, null, null);
                DockPane.getActiveDockPane().addTab(dockItem);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return appDescriptor;
    }

    public void raise(){
        dockItem.select();
    }
}
