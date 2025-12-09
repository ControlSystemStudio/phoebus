package org.phoebus.applications.queueserver.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.applications.queueserver.Preferences;
import org.phoebus.applications.queueserver.api.StatusResponse;
import org.phoebus.applications.queueserver.api.StatusWsMessage;
import org.phoebus.applications.queueserver.client.QueueServerWebSocket;
import org.phoebus.applications.queueserver.client.RunEngineService;
import org.phoebus.applications.queueserver.util.PollCenter;
import org.phoebus.applications.queueserver.util.StatusBus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReManagerConnectionController {

    @FXML private Button connectButton;
    @FXML private Button disconnectButton;
    @FXML private Label  connectionStatusLabel;

    private final RunEngineService svc = new RunEngineService();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private ScheduledFuture<?>   pollTask;
    private QueueServerWebSocket<StatusWsMessage> statusWs;
    private static final Logger logger = Logger.getLogger(ReManagerConnectionController.class.getPackageName());

    @FXML private void connect()    { start(); }
    @FXML private void disconnect() { stop();  }

    private void start() {
        if (pollTask != null && !pollTask.isDone()) return;     // already running
        if (statusWs != null && statusWs.isConnected()) return;  // already connected

        showPending();                                          // UI while waiting

        if (Preferences.use_websockets) {
            logger.log(Level.FINE, "Starting status WebSocket connection");
            startWebSocket();
        } else {
            logger.log(Level.FINE, "Starting status polling every " + Preferences.update_interval_ms + " milliseconds");
            updateWidgets(queryStatusOnce());
            pollTask = PollCenter.everyMs(Preferences.update_interval_ms,
                    this::queryStatusOnce,      // background
                    this::updateWidgets);       // FX thread
        }
    }

    private void startWebSocket() {
        statusWs = svc.createStatusWebSocket();

        statusWs.addListener(msg -> {
            Map<String, Object> statusMap = msg.status();
            if (statusMap != null) {
                try {
                    // Convert Map to StatusResponse
                    StatusResponse status = mapper.convertValue(statusMap, StatusResponse.class);
                    Platform.runLater(() -> updateWidgets(status));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to parse status from WebSocket", e);
                }
            }
        });

        statusWs.connect();
    }

    private StatusResponse queryStatusOnce() {
        try {
            return svc.status();
        } catch (Exception ex) {
            logger.log(Level.FINE, "Status query failed: " + ex.getMessage());
            return null;
        }
    }

    private void stop() {
        logger.log(Level.FINE, "Stopping status monitoring");
        if (pollTask != null) {
            pollTask.cancel(true);
            pollTask = null;
        }
        if (statusWs != null) {
            statusWs.disconnect();
            statusWs = null;
        }
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
            logger.log(Level.FINEST, "Status update: manager_state=" + s.managerState());
            showOnline();
        } else {
            showPending();      // keep polling; user may Disconnect
        }
    }
}
