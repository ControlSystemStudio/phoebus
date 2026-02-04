package org.phoebus.applications.queueserver.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EditAndControlQueueController implements Initializable {

    @FXML private AnchorPane runningPlanContainer;
    @FXML private AnchorPane planQueueContainer;
    @FXML private AnchorPane planHistoryContainer;
    
    private static final Logger logger = Logger.getLogger(EditAndControlQueueController.class.getPackageName());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.log(Level.FINE, "Initializing EditAndControlQueueController");
        loadInto(runningPlanContainer, "/org/phoebus/applications/queueserver/view/ReRunningPlan.fxml", new ReRunningPlanController(false));
        loadInto(planQueueContainer, "/org/phoebus/applications/queueserver/view/RePlanQueue.fxml", new RePlanQueueController(false));
        loadInto(planHistoryContainer, "/org/phoebus/applications/queueserver/view/RePlanHistory.fxml", new RePlanHistoryController(false));
    }

    private void loadInto(AnchorPane container, String fxml, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setController(controller);
            Parent view = loader.load();

            container.getChildren().setAll(view);
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load FXML: " + fxml, e);
        }
    }

}
