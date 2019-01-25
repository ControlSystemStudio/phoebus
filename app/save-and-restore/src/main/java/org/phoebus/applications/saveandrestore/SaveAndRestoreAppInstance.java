package org.phoebus.applications.saveandrestore;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

import javafx.scene.Node;

public class SaveAndRestoreAppInstance implements AppInstance {
	
	private AppDescriptor appDescriptor;
	
	public SaveAndRestoreAppInstance(AppDescriptor appDescriptor) {
		this.appDescriptor = appDescriptor;
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return appDescriptor;
	}
	

	public Node create() {
		return null;
	}

}
