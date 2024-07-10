package org.phoebus.applications.uxanalytics.ui;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.layout.VBox;
import org.phoebus.applications.uxanalytics.monitor.*;
import org.phoebus.applications.uxanalytics.monitor.backend.database.BackendConnection;
import org.phoebus.applications.uxanalytics.monitor.backend.database.MongoDBConnection;
import org.phoebus.applications.uxanalytics.monitor.backend.database.Neo4JConnection;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author Evan Daykin
 */

public class UXAnalyticsMain implements AppResourceDescriptor {
    public static final Logger logger = Logger.getLogger(UXAnalyticsMain.class.getPackageName());
    public static final String NAME = "uxanalyticsconfig";
    public static final String DISPLAY_NAME = "UX Analytics Config";
    private BackendConnection phoebusConnection = Neo4JConnection.getInstance();
    private BackendConnection jfxConnection = MongoDBConnection.getInstance();
    private final UXAMonitor monitor = UXAMonitor.getInstance();
    @Override
    public AppInstance create(URI resource) {
        return create();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public void start(){
        monitor.setPhoebusConnection(phoebusConnection);
        monitor.setJfxConnection(jfxConnection);
        logger.log(Level.INFO, "Load UX Analytics AppResource");
    }

    @Override
    public AppInstance create() {
        return null;
    }
}