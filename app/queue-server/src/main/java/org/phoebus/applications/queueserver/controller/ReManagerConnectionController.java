package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.api.StatusResponse;
import org.phoebus.applications.queueserver.client.RunEngineService;
import org.phoebus.applications.queueserver.util.PollCenter;
import org.phoebus.applications.queueserver.util.StatusBus;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReManagerConnectionController {

    @FXML private Button connectButton;
    @FXML private Button disconnectButton;
    @FXML private Label  connectionStatusLabel;

    private final RunEngineService svc = new RunEngineService();
    private ScheduledFuture<?>   pollTask;
    private static final int PERIOD_SEC = 1;
    private static final Logger LOG = Logger.getLogger(ReManagerConnectionController.class.getName());

    @FXML private void connect()    { startPolling(); }
    @FXML private void disconnect() { stopPolling();  }

    private void startPolling() {
        if (pollTask != null && !pollTask.isDone()) return;     // already running
        LOG.info("Starting connection polling every " + PERIOD_SEC + " seconds");
        showPending();                                          // UI while waiting

        updateWidgets(queryStatusOnce());

        pollTask = PollCenter.every(PERIOD_SEC,
                this::queryStatusOnce,      // background
                this::updateWidgets);       // FX thread
    }

    private StatusResponse queryStatusOnce() {
        try {
            return svc.status();
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Status query failed: " + ex.getMessage());
            return null;
        }
    }

    private void stopPolling() {
        LOG.info("Stopping connection polling");
        if (pollTask != null) pollTask.cancel(true);
        pollTask = null;
        StatusBus.push(null);
        showIdle();
    }

    private void showPending() {
        connectionStatusLabel.setText("-----");
        connectButton.setDisable(true);
        disconnectButton.setDisable(false);
    }
    private void showIdle() {
        connectionStatusLabel.setText("-----");
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
    }
    private void showOnline() {
        connectionStatusLabel.setText("ONLINE");
        connectButton.setDisable(true);
        disconnectButton.setDisable(false);
    }

    private void updateWidgets(StatusResponse s) {
        StatusBus.push((s));
        if (s != null) {
            LOG.log(Level.FINEST, "Status update: manager_state=" + s.managerState());
            showOnline();
        } else {
            showPending();      // keep polling; user may Disconnect
        }
    }
}
