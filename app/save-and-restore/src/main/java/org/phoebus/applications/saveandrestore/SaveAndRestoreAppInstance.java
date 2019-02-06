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
