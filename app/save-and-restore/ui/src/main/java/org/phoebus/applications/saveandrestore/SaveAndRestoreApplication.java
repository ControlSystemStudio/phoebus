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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import javafx.scene.Node;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class SaveAndRestoreApplication implements AppDescriptor, AppInstance {
	
	public static final String NAME = "Save And Restore";
	private AnnotationConfigApplicationContext context;
	private SaveAndRestoreController controller;

	public SaveAndRestoreApplication(){
		context = new AnnotationConfigApplicationContext(AppConfig.class);
	}

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

		SpringFxmlLoader springFxmlLoader = new SpringFxmlLoader();

		DockItem tab = null;

		try {
			tab = new DockItem(this, (Node)springFxmlLoader.load("ui/fxml/SaveAndRestoreUI.fxml"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		controller = springFxmlLoader.getLoader().getController();
		tab.setOnClosed(e -> controller.cleanUp());

		DockPane.getActiveDockPane().addTab(tab);
		PreferencesReader reader = (PreferencesReader)context.getBean("preferencesReader");
		String epicsAddressList = reader.get("addr_list");
		if(!Strings.isNullOrEmpty(epicsAddressList)){
			System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", epicsAddressList);
		}
		return this;
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return this;
	}

	@Override
	public void save(Memento memento){
		controller.cleanUp();
	}
}
