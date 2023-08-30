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


package org.phoebus.applications.saveandrestore;

import javafx.scene.input.DataFormat;
import javafx.stage.Stage;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;

import java.net.URI;

/**
 * Implemented such that only one single instance of the application will be launched.
 */
public class SaveAndRestoreApplication implements AppResourceDescriptor {

    public static final String NAME = "saveandrestore";
    public static final String DISPLAY_NAME = "Save And Restore";

    /**
     * Custom MIME type definition for the purpose of drag-n-drop in the
     */
    public static final DataFormat NODE_SELECTION_FORMAT = new DataFormat("application/node-selection");

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }


    @Override
    public AppInstance create() {
        return create(null);
    }


    @Override
    public AppInstance create(URI uri) {

		// Check if save & restore app is already running
		for (Stage stage : DockStage.getDockStages()) {
			for (DockPane pane : DockStage.getDockPanes(stage)) {
				for (DockItem tab : pane.getDockItems()) {
					if (tab.getApplication().getAppDescriptor().getName().equals(NAME)) {
                        tab.select();
                        if(uri != null){
                            ((SaveAndRestoreInstance)tab.getApplication()).openResource(uri);
                        }
                        return tab.getApplication();
					}
				}
			}
		}

		return new SaveAndRestoreInstance(this, uri);
    }
}
