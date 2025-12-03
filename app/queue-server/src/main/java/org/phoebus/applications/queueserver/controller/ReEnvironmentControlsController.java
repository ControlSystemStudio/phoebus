package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.api.StatusResponse;
import org.phoebus.applications.queueserver.client.RunEngineService;
import org.phoebus.applications.queueserver.util.StatusBus;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReEnvironmentControlsController implements Initializable {

    @FXML private Button openBtn;
    @FXML private Button closeBtn;
    @FXML private Button destroyBtn;

    private final RunEngineService svc = new RunEngineService();
    private static final Logger logger = Logger.getLogger(ReEnvironmentControlsController.class.getPackageName());

    @Override public void initialize(URL url, ResourceBundle rb) {
        StatusBus.latest().addListener(this::onStatus);
    }

    private void onStatus(ObservableValue<?> src, Object oldV, Object newV) {
        Platform.runLater(() -> refreshButtons(newV));
    }

    @SuppressWarnings("unchecked")
    private void refreshButtons(Object statusObj) {

        boolean connected     = statusObj != null;
        boolean workerExists  = false;
        String  managerState  = null;

        if (statusObj instanceof StatusResponse s) {          // preferred
            workerExists = s.workerEnvironmentExists();
            managerState = s.managerState();
        } else if (statusObj instanceof Map<?,?> m) {         // fall-back
            workerExists = Boolean.TRUE.equals(m.get("worker_environment_exists"));
            managerState = String.valueOf(m.get("manager_state"));
        }

        boolean idle = "idle".equals(managerState);

        openBtn   .setDisable(!(connected && !workerExists && idle));
        closeBtn  .setDisable(!(connected &&  workerExists && idle));
        destroyBtn.setDisable(!(connected &&  workerExists && idle));
    }

    @FXML private void open() {
        try { svc.environmentOpen(); }
        catch (Exception ex) { logger.log(Level.WARNING, "environmentOpen: " + ex); }
    }

    @FXML private void close() {
        try { svc.environmentClose(); }
        catch (Exception ex) { logger.log(Level.WARNING, "environmentClose: " + ex); }
    }

    @FXML private void destroy() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Destroy Environment");
        alert.setHeaderText("Are you sure you want to destroy the environment?");
        alert.setContentText("This action cannot be undone and will permanently destroy the current environment.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try { 
                    svc.environmentDestroy(); 
                    logger.log(Level.FINE, "Environment destroyed successfully");
                } catch (Exception ex) { 
                    logger.log(Level.WARNING, "environmentDestroy: " + ex); 
                }
            }
        });
    }
}
