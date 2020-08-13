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


import javafx.scene.Node;
import org.phoebus.applications.saveandrestore.ui.BaseSaveAndRestoreController;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.pv.ca.JCA_Preferences;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SaveAndRestoreApplication implements AppDescriptor, AppInstance {
	
	public static final String NAME = "Save And Restore";
	private AnnotationConfigApplicationContext context;
	private BaseSaveAndRestoreController controller;

	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public String getDisplayName() {
		return NAME;
	}

	@Override
	public AppInstance create() {

		context = new AnnotationConfigApplicationContext(AppConfig.class);

		SpringFxmlLoader springFxmlLoader = new SpringFxmlLoader();

		DockItem tab = null;

		PreferencesReader preferencesReader = (PreferencesReader) context.getBean("preferencesReader");
		try {
		    if (preferencesReader.getBoolean("splitSnapshot")) {
				tab = new DockItem(this, (Node) springFxmlLoader.load("ui/SaveAndRestoreUIWithSplit.fxml"));
			} else {
				tab = new DockItem(this, (Node) springFxmlLoader.load("ui/SaveAndRestoreUI.fxml"));
			}
		} catch (Exception e) {
			Logger.getLogger(SaveAndRestoreApplication.class.getName()).log(Level.SEVERE, "Failed loading fxml", e);
		}

		controller = springFxmlLoader.getLoader().getController();

		tab.setOnCloseRequest(event -> controller.closeTagSearchWindow());

		DockPane.getActiveDockPane().addTab(tab);
		try {
			JCA_Preferences.getInstance().installPreferences();
		} catch (Exception e) {
			Logger.getLogger(SaveAndRestoreApplication.class.getName()).log(Level.SEVERE, "Failed loading JCA preferences", e);
		}

		return this;
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return this;
	}

	@Override
	public void save(Memento memento){
		controller.save(memento);
	}

	@Override
	public void restore(final Memento memento) {
		controller.restore(memento);
	}
}
