package org.phoebus.applications.display.navigator;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

public class AddMenuEntry implements MenuEntry {

    @Override
    public Void call() {
        ApplicationService.createInstance("navigator");
        return null;
    }

    @Override
    public String getName() {
        return "Navigator";
    }

    @Override
    public String getMenuPath() {
        return "Display";
    }

    @Override
    public Image getIcon() {
        return ImageCache.getImage(getClass(), "/icons/navigator.png");
    }

}