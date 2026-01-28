package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.api.StatusResponse;
import org.phoebus.applications.queueserver.client.RunEngineService;
import org.phoebus.applications.queueserver.util.StatusBus;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReExecutionControlsController implements Initializable {

    @FXML private Button pauseDefBtn, pauseImmBtn, resumeBtn, ctrlcBtn,
            stopBtn, abortBtn, haltBtn;

    private final RunEngineService svc = new RunEngineService();
    private static final Logger logger = Logger.getLogger(ReExecutionControlsController.class.getPackageName());

    @Override public void initialize(URL url, ResourceBundle rb) {

        StatusBus.latest().addListener(this::onStatus);

        pauseDefBtn.setOnAction(e -> call("rePause deferred",
                () -> {
                    try {
                        svc.rePause("deferred");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }));                        // server default=deferred
        pauseImmBtn.setOnAction(e -> call("rePause immediate",
                () -> {
                    try {
                        svc.rePause("immediate");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }));                        // same endpoint (no body)
        resumeBtn.setOnAction(e -> call("reResume", () -> {
            try { svc.reResume(); } catch (Exception ex) { throw new RuntimeException(ex); }
        }));
        stopBtn.setOnAction(e -> call("reStop", () -> {
            try { svc.reStop(); }   catch (Exception ex) { throw new RuntimeException(ex); }
        }));
        abortBtn.setOnAction(e -> call("reAbort", () -> {
            try { svc.reAbort(); }  catch (Exception ex) { throw new RuntimeException(ex); }
        }));
        haltBtn.setOnAction(e -> call("reHalt", () -> {
            try { svc.reHalt(); }   catch (Exception ex) { throw new RuntimeException(ex); }
        }));
        ctrlcBtn.setOnAction(e -> call("kernelInterrupt", () -> {
            try { svc.kernelInterrupt(); } catch (Exception ex) { throw new RuntimeException(ex); }
        }));
    }


    private void onStatus(ObservableValue<?> src, Object oldV, Object newV) {
        Platform.runLater(() -> refreshButtons(newV));
    }

    @SuppressWarnings("unchecked")
    private void refreshButtons(Object statusObj) {
        boolean connected = statusObj != null;

        boolean workerExists     = false;
        String  managerState     = null;
        String  reState          = null;
        String  ipKernelState    = null;

        if (statusObj instanceof StatusResponse s) {
            workerExists  = s.workerEnvironmentExists();
            managerState  = s.managerState();
            reState       = s.reState();
            ipKernelState = s.ipKernelState();
        }
        else if (statusObj instanceof Map<?,?> m) {
            workerExists  = Boolean.TRUE.equals(m.get("worker_environment_exists"));
            managerState  = String.valueOf(m.get("manager_state"));
            reState       = String.valueOf(m.get("re_state"));
            ipKernelState = String.valueOf(m.get("ip_kernel_state"));
        }

        boolean pausePossible =
                "executing_queue".equals(managerState) || "running".equals(reState);

        pauseDefBtn.setDisable(!(connected && workerExists && pausePossible));
        pauseImmBtn.setDisable(!(connected && workerExists && pausePossible));

        boolean paused = "paused".equals(managerState);
        resumeBtn.setDisable(!(connected && workerExists && paused));

        stopBtn .setDisable(!(connected && workerExists && paused));
        abortBtn.setDisable(!(connected && workerExists && paused));
        haltBtn .setDisable(!(connected && workerExists && paused));

        boolean ipIdleOrBusy = "idle".equals(ipKernelState) || "busy".equals(ipKernelState);
        boolean ctrlCEnabled = connected && workerExists &&
                !"executing_queue".equals(managerState) &&
                !"running".equals(reState) &&
                ipIdleOrBusy;
        ctrlcBtn.setDisable(!ctrlCEnabled);
    }

    private void call(String label, Runnable r) {
        try { r.run(); }
        catch (Exception ex) { logger.log(Level.WARNING, label + ": " + ex); }
    }
}
