package org.phoebus.applications.uxanalytics.ui;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author Evan Daykin
 */

public class UXAnalyticsUI implements AppResourceDescriptor {
    public static final Logger logger = Logger.getLogger(UXAnalyticsUI.class.getPackageName());
    public static final String NAME = "uxanalyticsconfig";
    public static final String DISPLAY_NAME = "UX Analytics Config";

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
       System.out.println("Starting my thing");
    }

    @Override
    public AppInstance create() {
        try{
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(UXAnalyticsUI.class.getResource("uxa-settings-dialog.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 600, 800);
            Stage stage = new Stage();
            stage.setTitle("Test");
            stage.setScene(scene);
            stage.show();
        }
        catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create UX Analytics dialog", e);
        }
        return null;
    }
}