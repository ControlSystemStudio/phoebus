package org.csstudio.display.builder.model;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;


@SuppressWarnings("nls")
public class ConverterMenuEntry implements MenuEntry {

	
	@Override
	public Void call() throws Exception {
		ApplicationService.createInstance(AdvancedConverterApp.NAME);
		return null;
	}

	@Override
	public String getMenuPath() {
		// TODO Auto-generated method stub
		return "Utility";
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return AdvancedConverterApp.DisplayName;
	}
	
    public Image getIcon()
    {
    	 return ImageCache.getImage(AdvancedConverterApp.class, "/icons/arrow_refresh.png");
    }

    
   

}
