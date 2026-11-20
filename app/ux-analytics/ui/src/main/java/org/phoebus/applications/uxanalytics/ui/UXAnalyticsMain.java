package org.phoebus.applications.uxanalytics.ui;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.phoebus.applications.uxanalytics.monitor.*;
import org.phoebus.applications.uxanalytics.monitor.backend.database.BackendConnection;
import org.phoebus.applications.uxanalytics.monitor.backend.database.ServiceLayerConnection;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.workbench.ApplicationService;

/**
 * @author Evan Daykin
 */

public class UXAnalyticsMain implements AppResourceDescriptor {
    public static final Logger logger = Logger.getLogger(UXAnalyticsMain.class.getPackageName());
    public static final String NAME = "uxanalyticsconfig";
    public static final String DISPLAY_NAME = "UX Analytics Config";
    private BackendConnection phoebusConnection = ServiceLayerConnection.getInstance();
    private BackendConnection jfxConnection = ServiceLayerConnection.getInstance();
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
        boolean consent = ConsentPersistence.getConsent();
        if(!ConsentPersistence.consentIsPersistent()){
            Platform.runLater(() -> {
                ApplicationService.createInstance(NAME);
            });
        }
        monitor.setPhoebusConnection(phoebusConnection);
        monitor.setJfxConnection(jfxConnection);
        if(consent){
            monitor.enableTracking();
        }
        else{
            monitor.disableTracking();
        }
        logger.log(Level.FINE, "Loaded UX Analytics plugin with consent: " + consent);
    }

    @Override
    public AppInstance create() {
        try{
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(UXAnalyticsMain.class.getResource("/org/phoebus/applications/uxanalytics/ui/uxa-settings-dialog.fxml"));
            loader.setController(new UXAController(phoebusConnection));
            Parent root = loader.load();
            final UXAController controller = loader.getController();
            controller.setObserver(monitor);
            Scene scene = new Scene(root,400,200);
            Stage stage = new Stage();
            stage.setTitle("Analytics Opt-In");
            stage.setScene(scene);
            stage.show();
            stage.toFront();
        }
        catch (Exception e){
            logger.log(Level.WARNING, "Failed to create UX Analytics dialog", e);
        }
    return null;
    }
}