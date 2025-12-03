package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.api.StatusResponse;
import org.phoebus.applications.queueserver.client.RunEngineService;
import org.phoebus.applications.queueserver.util.StatusBus;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReQueueControlsController implements Initializable {

    @FXML private CheckBox autoChk;
    @FXML private Label    reStatusLabel;
    @FXML private Button   startBtn;
    @FXML private Button   stopBtn;            // text toggles “Stop” / “Cancel Stop”

    private final RunEngineService svc = new RunEngineService();
    private static final Logger logger = Logger.getLogger(ReQueueControlsController.class.getPackageName());

    private volatile boolean autoEnabledSrv   = false;   // what server says now
    private volatile boolean stopPendingSrv   = false;   // ditto

    @Override public void initialize(URL url, ResourceBundle rb) {

        StatusBus.latest().addListener(this::onStatus);

        startBtn.setOnAction(e -> runSafely("queueStart",  () -> {
            try {
                svc.queueStart();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }));

        stopBtn.setOnAction(e -> {
            if (!stopPendingSrv)
                runSafely("queueStop",        () -> {
                    try {
                        svc.queueStop();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
            else
                runSafely("queueStopCancel",  () -> {
                    try {
                        svc.queueStopCancel();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
        });

        autoChk.setOnAction(e -> {
            boolean want = autoChk.isSelected();
            if (want != autoEnabledSrv) {
                runSafely("queueAutostart",
                        () -> {
                            try {
                                svc.queueAutostart(Map.of("enable", want));
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        });
            }
        });
    }

    private void onStatus(ObservableValue<?> src, Object oldV, Object newV) {
        Platform.runLater(() -> refreshButtons(newV));
    }

    @SuppressWarnings("unchecked")
    private void refreshButtons(Object statusObj) {

        boolean connected           = statusObj != null;
        boolean workerExists        = false;
        boolean running             = false;
        String  managerState        = null;
        boolean queueStopPending    = false;
        boolean queueAutoEnabled    = false;

        if (statusObj instanceof StatusResponse s) {
            workerExists     = s.workerEnvironmentExists();
            running          = s.runningItemUid() != null;
            managerState     = s.managerState();
            queueStopPending = s.queueStopPending();
            queueAutoEnabled = s.queueAutostartEnabled();
        }
        else if (statusObj instanceof Map<?,?> m) {
            workerExists     = Boolean.TRUE.equals(m.get("worker_environment_exists"));
            running          = m.get("running_item_uid") != null;
            managerState     = String.valueOf(m.get("manager_state"));
            queueStopPending = Boolean.TRUE.equals(m.get("queue_stop_pending"));
            queueAutoEnabled = Boolean.TRUE.equals(m.get("queue_autostart_enabled"));
        }

        autoEnabledSrv = queueAutoEnabled;
        stopPendingSrv = queueStopPending;

        boolean idle   = "idle".equals(managerState);

        reStatusLabel.setText(running ? "RUNNING" : "STOPPED");

        boolean startEnabled =
                connected && workerExists && !running && !queueAutoEnabled;
        startBtn.setDisable(!startEnabled);

        stopBtn.setText(queueStopPending ? "Cancel Stop" : "Stop");
        stopBtn.setDisable(!(connected && workerExists && running));

        autoChk.setDisable(!(connected && workerExists));
        autoChk.setSelected(queueAutoEnabled);
    }

    private void runSafely(String what, Runnable r) {
        try { r.run(); }
        catch (Exception ex) { logger.log(Level.WARNING, what + ": " + ex); }
    }
}
