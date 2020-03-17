package org.phoebus.applications.display.navigation;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;

import java.net.URI;

public class DisplayNavigationView implements AppInstance {

    private DockItem tab;
    private final AppDescriptor app;

    public DisplayNavigationView(AppDescriptor app) {
        this.app = app;
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return app;
    }


    public void setResource(URI resource) {

    }
}
