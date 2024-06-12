package org.phoebus.applications.uxanalytics.ui;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.layout.VBox;
import org.phoebus.applications.uxanalytics.monitor.BackendConnection;
import org.phoebus.applications.uxanalytics.monitor.MongoDBConnection;
import org.phoebus.applications.uxanalytics.monitor.Neo4JConnection;
import org.phoebus.applications.uxanalytics.monitor.UXAMonitor;
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
    private BackendConnection phoebusConnection = new Neo4JConnection();
    private BackendConnection jfxConnection = new MongoDBConnection();
    private final UXAController neo4jController = new UXAController(phoebusConnection);
    private final UXAController mongoController = new UXAController(jfxConnection);
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
        try{
            final FXMLLoader log4JLoader = new FXMLLoader();
            log4JLoader.setLocation(UXAnalyticsMain.class.getResource("uxa-settings-dialog.fxml"));
            log4JLoader.setController(neo4jController);
            Parent log4JRoot = log4JLoader.load();
            neo4jController.setObserver(monitor);


            final FXMLLoader mongoLoader = new FXMLLoader();
            mongoLoader.setLocation(UXAnalyticsMain.class.getResource("uxa-settings-dialog.fxml"));
            mongoLoader.setController(mongoController);
            Parent mongoRoot = mongoLoader.load();
            mongoController.setObserver(monitor);

            VBox root = new VBox(log4JRoot, mongoRoot);
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle("UX Analytics Configuration");
            stage.setScene(scene);
            neo4jController.initialize();
            mongoController.initialize();
            stage.show();
        }
        catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create UX Analytics dialog", e);
        }
        return null;
    }
}