package org.phoebus.applications.display.navigation;

import javafx.fxml.FXMLLoader;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;

public class DisplayNavigationView implements AppInstance {

    private DockItem tab;
    private final AppDescriptor app;

    private DisplayNavigationViewController controller;

    public DisplayNavigationView(AppDescriptor app) {
        this.app = app;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("DisplayNavigationView.fxml"));
            loader.load();
            controller = loader.getController();

            tab = new DockItem(this, loader.getRoot());
            DockPane.getActiveDockPane().addTab(tab);
        } catch (IOException e) {
            DisplayNavigationViewApp.logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return app;
    }

    public void setResource(URI resource) {
        controller.setRootFile(new File(resource));
    }
}
