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
package org.phoebus.applications.saveandrestore;

import java.io.IOException;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

public class SaveAndRestoreAppInstance implements AppInstance {
	
	private AppDescriptor appDescriptor;
	
	public SaveAndRestoreAppInstance(AppDescriptor appDescriptor) {
		this.appDescriptor = appDescriptor;
		
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(this.getClass().getResource("ui/fxml/SaveAndRestoreUI.fxml"));
			DockItem tab = new DockItem(this, loader.load());
			
			DockPane.getActiveDockPane().addTab(tab);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return appDescriptor;
	}
	

	public Node create() {
		return null;
	}

}
