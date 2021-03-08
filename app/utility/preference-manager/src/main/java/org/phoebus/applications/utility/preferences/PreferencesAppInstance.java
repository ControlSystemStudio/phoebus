package org.phoebus.applications.utility.preferences;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import java.io.IOException;
import java.util.logging.Level;

import static org.phoebus.applications.utility.preferences.PreferencesApp.logger;

public class PreferencesAppInstance implements AppInstance {

    public static PreferencesAppInstance INSTANCE = null;

    private final AppDescriptor app;
    private DockItem tab;

    public PreferencesAppInstance(AppDescriptor app)
    {
        this.app = app;
        tab = new DockItem(this, createFxScene());
        tab.addClosedNotification(() -> INSTANCE = null);
        DockPane.getActiveDockPane().addTab(tab);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    void raise()
    {
        DockPane.getActiveDockPane().addTab(tab);
    }


    protected Node createFxScene() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("PreferencesTree.fxml"));
            loader.load();
            return loader.getRoot();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Cannot load UI", e);
        }
        return null;
    }
}
