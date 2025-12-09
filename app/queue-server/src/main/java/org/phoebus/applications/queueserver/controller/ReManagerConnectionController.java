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
    private ScheduledFuture<?>   timeoutTask;
    private ScheduledFuture<?>   healthCheckTask;
    private QueueServerWebSocket<StatusWsMessage> statusWs;
    private volatile StatusResponse latestStatus = null;
    private volatile boolean connected = false;
    private volatile long lastStatusUpdateTime = 0;
    private static final Logger logger = Logger.getLogger(ReManagerConnectionController.class.getPackageName());

    @FXML private void connect()    { start(); }
    @FXML private void disconnect() { stop();  }

    private void start() {
        if (pollTask != null && !pollTask.isDone()) return;     // already running
        if (statusWs != null && statusWs.isConnected()) return;  // already connected

        connected = false;
        latestStatus = null;
        showConnecting();

        // Set up connection timeout (one-time check after the timeout period)
        if (Preferences.connectTimeout > 0) {
            timeoutTask = PollCenter.afterMs(Preferences.connectTimeout, this::checkConnectionTimeout);
        }

        if (Preferences.use_websockets) {
            logger.log(Level.FINE, "Starting status WebSocket connection (timeout: " + Preferences.connectTimeout + "ms)");
            startWebSocket();
        } else {
            logger.log(Level.FINE, "Starting status polling every " + Preferences.update_interval_ms + " milliseconds (timeout: " + Preferences.connectTimeout + "ms)");
            pollTask = PollCenter.everyMs(Preferences.update_interval_ms,
                    this::queryStatusOnce,      // background
                    this::updateWidgets);       // FX thread
        }
    }

    private void startWebSocket() {
        statusWs = svc.createStatusWebSocket();

        // Buffer incoming WebSocket messages without immediately updating UI
        statusWs.addListener(msg -> {
            Map<String, Object> statusMap = msg.status();
            if (statusMap != null) {
                try {
                    // Convert Map to StatusResponse and buffer it
                    latestStatus = mapper.convertValue(statusMap, StatusResponse.class);
                    // Record timestamp when we receive fresh data from WebSocket
                    lastStatusUpdateTime = System.currentTimeMillis();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to parse status from WebSocket", e);
                }
            }
        });

        statusWs.connect();

        // Schedule throttled UI updates at the configured interval
        pollTask = PollCenter.everyMs(Preferences.update_interval_ms, () -> {
            // Check if WebSocket is still connected
            if (statusWs != null && !statusWs.isConnected()) {
                // WebSocket closed - clear buffer and stop receiving updates
                latestStatus = null;
            }

            StatusResponse status = latestStatus;
            if (status != null) {
                Platform.runLater(() -> updateWidgets(status));
            }
        });
    }

    private StatusResponse queryStatusOnce() {
        try {
            return svc.status();
        } catch (Exception ex) {
            logger.log(Level.FINE, "Status query failed: " + ex.getMessage());
            return null;
        }
    }

    private void checkConnectionTimeout() {
        if (!connected) {
            logger.log(Level.WARNING, "Connection timeout after " + Preferences.connectTimeout + "ms");
            Platform.runLater(() -> {
                stop();
                showError("TIMEOUT");
            });
        }
    }

    private void checkConnectionHealth() {
        if (connected && Preferences.connectTimeout > 0) {
            long timeSinceLastUpdate = System.currentTimeMillis() - lastStatusUpdateTime;
            if (timeSinceLastUpdate > Preferences.connectTimeout) {
                logger.log(Level.WARNING, "Connection lost - no status update for " + timeSinceLastUpdate + "ms");
                Platform.runLater(() -> {
                    stop();
                    showError("TIMEOUT");
                });
            }
        }
    }

    private void stop() {
        logger.log(Level.FINE, "Stopping status monitoring");
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
            timeoutTask = null;
        }
        if (healthCheckTask != null) {
            healthCheckTask.cancel(true);
            healthCheckTask = null;
        }
        if (pollTask != null) {
            pollTask.cancel(true);
            pollTask = null;
        }
        if (statusWs != null) {
            statusWs.disconnect();
            statusWs = null;
        }
        connected = false;
        latestStatus = null;
        lastStatusUpdateTime = 0;
        StatusBus.push(null);
        showIdle();
    }

    private void showConnecting() {
        connectionStatusLabel.setText("CONNECTING");
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

    private void showError(String message) {
        connectionStatusLabel.setText(message);
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
    }

    private void updateWidgets(StatusResponse s) {
        StatusBus.push((s));
        if (s != null) {
            logger.log(Level.FINEST, "Status update: manager_state=" + s.managerState());

            // Record timestamp of successful status update (only for HTTP mode)
            // For WebSocket mode, timestamp is already updated when message is received
            if (!Preferences.use_websockets) {
                lastStatusUpdateTime = System.currentTimeMillis();
            }

            // Successfully connected - cancel initial timeout and start health monitoring
            if (!connected) {
                connected = true;
                if (timeoutTask != null) {
                    timeoutTask.cancel(false);
                    timeoutTask = null;
                }

                // Start periodic health check to detect server disconnect
                if (Preferences.connectTimeout > 0 && healthCheckTask == null) {
                    logger.log(Level.FINE, "Starting connection health monitoring");
                    healthCheckTask = PollCenter.everyMs(1000, this::checkConnectionHealth);
                }
            }

            showOnline();
        } else {
            // Only show connecting if we haven't timed out yet
            if (timeoutTask != null && !timeoutTask.isDone()) {
                showConnecting();
            }
        }
    }
}
