package org.phoebus.applications.display.navigation;

import javafx.scene.image.Image;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.MenuEntry;

public class DisplayNavigationViewMenuEntry implements MenuEntry {

    @Override
    public String getName() {
        return DisplayNavigationViewApp.DISPLAYNAME;
    }

    @Override
    public String getMenuPath() {
        return "Utility";
    }

    @Override
    public Void call() throws Exception {
        ApplicationService.findApplication(DisplayNavigationViewApp.NAME).create();
        return null;
    }

    @Override
    public Image getIcon() {
        return DisplayNavigationViewApp.icon;
    }
}
