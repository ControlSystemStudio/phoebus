package org.phoebus.applications.greetings;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.fxml.FXMLLoader;

@SuppressWarnings("nls")
public class Greet implements AppInstance {
    private final AppDescriptor app;

    Greet(final AppDescriptor app)
    {
        this.app = app;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("ui/GreetingView.fxml"));
            final DockItem tab = new DockItem(this, loader.load());
            DockPane.getActiveDockPane().addTab(tab);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot load UI", e);
        }
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }
}
