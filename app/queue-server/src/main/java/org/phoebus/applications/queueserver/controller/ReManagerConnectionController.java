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
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReManagerConnectionController {

    @FXML private ToggleButton autoConnectToggle;
    @FXML private Label connectionStatusLabel;

    private final RunEngineService svc = new RunEngineService();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private ScheduledFuture<?>   pollTask;
    private ScheduledFuture<?>   reconnectTask;
    private ScheduledFuture<?>   healthCheckTask;
    private QueueServerWebSocket<StatusWsMessage> statusWs;
    private volatile StatusResponse latestStatus = null;
    private volatile boolean websocketConnected = false;
    private volatile boolean attemptingConnection = false;
    private volatile long lastStatusUpdateTime = 0;
    private volatile long connectionAttemptStartTime = 0;
    private volatile int reconnectAttempts = 0;
    private volatile ConnectionState currentState = ConnectionState.DISCONNECTED;
    private static final Logger logger = Logger.getLogger(ReManagerConnectionController.class.getPackageName());

    // Status message timeout - if no status received for this long, show RED error
    private static final long STATUS_TIMEOUT_MS = 3000;

    // Connection attempt timeout - if no connection after this long, show NETWORK ERROR
    private static final long CONNECTION_ATTEMPT_TIMEOUT_MS = 5000;

    // Reconnect interval - how often to retry connection when it fails
    private static final long RECONNECT_INTERVAL_MS = 5000;

    // Connection states
    private enum ConnectionState {
        DISCONNECTED,    // Grey - User manually disabled auto-connect
        CONNECTING,      // Grey - Attempting to establish connection
        NETWORK_ERROR,   // Dark blue - WebSocket can't connect (auth/network issue)
        NO_STATUS,       // Red - WebSocket connected but no status messages from RE Manager
        CONNECTED        // Green - WebSocket connected AND receiving status messages
    }

    @FXML
    public void initialize() {
        // Start in auto-connect mode (toggle is selected by default in FXML)
        if (autoConnectToggle.isSelected()) {
            start();
        }
    }

    @FXML
    private void toggleConnection() {
        if (autoConnectToggle.isSelected()) {
            // User enabled auto-connect
            start();
        } else {
            // User disabled auto-connect
            stop();
        }
    }

    private void start() {
        if (pollTask != null && !pollTask.isDone()) return;     // already running
        if (statusWs != null && statusWs.isConnected()) return;  // already connected

        websocketConnected = false;
        attemptingConnection = false;
        latestStatus = null;
        lastStatusUpdateTime = 0;
        connectionAttemptStartTime = 0;
        reconnectAttempts = 0;

        if (Preferences.use_websockets) {
            logger.log(Level.INFO, "Starting status WebSocket connection");
            attemptWebSocketConnection();
        } else {
            logger.log(Level.INFO, "Starting status polling every " + Preferences.update_interval_ms + " milliseconds");
            attemptingConnection = true;
            connectionAttemptStartTime = System.currentTimeMillis();
            pollTask = PollCenter.everyMs(Preferences.update_interval_ms,
                    this::queryStatusOnce,      // background
                    this::updateWidgets);       // FX thread
        }

        // Start health check to monitor connection state
        if (healthCheckTask == null) {
            healthCheckTask = PollCenter.everyMs(500, this::checkConnectionHealth);
        }
    }

    private void attemptWebSocketConnection() {
        try {
            attemptingConnection = true;
            connectionAttemptStartTime = System.currentTimeMillis();

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

                        // Mark WebSocket as connected on first successful message
                        if (!websocketConnected) {
                            websocketConnected = true;
                            attemptingConnection = false;
                            reconnectAttempts = 0;
                            logger.log(Level.INFO, "WebSocket connection established");
                        }
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
                    // WebSocket closed
                    if (websocketConnected) {
                        logger.log(Level.WARNING, "WebSocket connection lost");
                        websocketConnected = false;
                        attemptingConnection = false;

                        // Auto-reconnect if toggle is still enabled
                        if (autoConnectToggle.isSelected()) {
                            scheduleReconnect();
                        }
                    }
                }

                StatusResponse status = latestStatus;
                if (status != null) {
                    Platform.runLater(() -> updateWidgets(status));
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create WebSocket connection", e);
            websocketConnected = false;
            attemptingConnection = false;

            // Auto-reconnect if toggle is still enabled
            if (autoConnectToggle.isSelected()) {
                scheduleReconnect();
            }
        }
    }

    private void scheduleReconnect() {
        // Don't reconnect if user disabled auto-connect
        if (!autoConnectToggle.isSelected()) {
            return;
        }

        // Don't schedule if already scheduled and not done
        if (reconnectTask != null && !reconnectTask.isDone()) {
            logger.log(Level.FINE, "Reconnect already scheduled, skipping");
            return;
        }

        // Don't schedule if already attempting connection
        if (attemptingConnection) {
            logger.log(Level.FINE, "Connection attempt in progress, skipping reconnect schedule");
            return;
        }

        // Cancel any existing reconnect task
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
        }

        reconnectAttempts++;

        logger.log(Level.INFO, "Scheduling reconnect attempt #" + reconnectAttempts + " in " + RECONNECT_INTERVAL_MS + "ms");

        reconnectTask = PollCenter.afterMs((int) RECONNECT_INTERVAL_MS, () -> {
            if (autoConnectToggle.isSelected() && !attemptingConnection) {
                logger.log(Level.INFO, "Attempting to reconnect (attempt #" + reconnectAttempts + ")");

                // Clean up existing connection
                if (statusWs != null) {
                    statusWs.disconnect();
                    statusWs = null;
                }
                if (pollTask != null) {
                    pollTask.cancel(false);
                    pollTask = null;
                }

                // Reset state before attempting reconnection
                websocketConnected = false;

                // Try to reconnect
                attemptWebSocketConnection();
            }
        });
    }

    private StatusResponse queryStatusOnce() {
        try {
            StatusResponse status = svc.status();
            if (status != null) {
                lastStatusUpdateTime = System.currentTimeMillis();
                if (attemptingConnection) {
                    attemptingConnection = false;
                    reconnectAttempts = 0;
                    logger.log(Level.INFO, "HTTP connection established");
                }
            }
            return status;
        } catch (Exception ex) {
            logger.log(Level.FINE, "Status query failed: " + ex.getMessage());
            return null;
        }
    }

    private void checkConnectionHealth() {
        ConnectionState newState;

        if (!autoConnectToggle.isSelected()) {
            // User disabled auto-connect
            newState = ConnectionState.DISCONNECTED;
        } else if (Preferences.use_websockets) {
            // WebSocket mode
            if (attemptingConnection) {
                // Currently attempting to connect
                long timeSinceAttemptStart = System.currentTimeMillis() - connectionAttemptStartTime;
                if (timeSinceAttemptStart > CONNECTION_ATTEMPT_TIMEOUT_MS) {
                    // Connection attempt timed out - trigger reconnect
                    attemptingConnection = false;
                    newState = ConnectionState.NETWORK_ERROR;

                    // Clean up failed connection attempt
                    if (pollTask != null) {
                        pollTask.cancel(false);
                        pollTask = null;
                    }
                    if (statusWs != null) {
                        statusWs.disconnect();
                        statusWs = null;
                    }

                    scheduleReconnect();
                } else {
                    // Still trying to connect
                    newState = ConnectionState.CONNECTING;
                }
            } else if (!websocketConnected) {
                // Not attempting and not connected
                newState = ConnectionState.NETWORK_ERROR;

                // Only schedule reconnect if no reconnect task is pending
                if ((reconnectTask == null || reconnectTask.isDone()) &&
                    (statusWs == null || !statusWs.isConnected())) {
                    scheduleReconnect();
                }
            } else if (lastStatusUpdateTime == 0) {
                // WebSocket connected but no status received yet - still establishing
                newState = ConnectionState.CONNECTING;
            } else {
                long timeSinceLastUpdate = System.currentTimeMillis() - lastStatusUpdateTime;
                if (timeSinceLastUpdate > STATUS_TIMEOUT_MS) {
                    // WebSocket connected but no recent status - RE Manager issue
                    newState = ConnectionState.NO_STATUS;
                } else {
                    // All good - connected and receiving status
                    newState = ConnectionState.CONNECTED;
                }
            }
        } else {
            // HTTP polling mode
            if (attemptingConnection) {
                // Currently attempting to connect
                long timeSinceAttemptStart = System.currentTimeMillis() - connectionAttemptStartTime;
                if (timeSinceAttemptStart > CONNECTION_ATTEMPT_TIMEOUT_MS) {
                    // Connection attempt timed out
                    attemptingConnection = false;
                    newState = ConnectionState.NETWORK_ERROR;
                } else {
                    // Still trying to connect
                    newState = ConnectionState.CONNECTING;
                }
            } else if (lastStatusUpdateTime == 0) {
                // No successful status query yet
                newState = ConnectionState.NETWORK_ERROR;
            } else {
                long timeSinceLastUpdate = System.currentTimeMillis() - lastStatusUpdateTime;
                if (timeSinceLastUpdate > STATUS_TIMEOUT_MS) {
                    // No recent status
                    newState = ConnectionState.NETWORK_ERROR;
                } else {
                    // Receiving status
                    newState = ConnectionState.CONNECTED;
                }
            }
        }

        // Only update UI if state actually changed
        if (newState != currentState) {
            ConnectionState oldState = currentState;
            currentState = newState;
            logger.log(Level.FINE, "Connection state change: " + oldState + " → " + newState);
            Platform.runLater(() -> updateConnectionState(newState));
        }
    }

    private void stop() {
        logger.log(Level.INFO, "Stopping status monitoring");

        if (reconnectTask != null) {
            reconnectTask.cancel(true);
            reconnectTask = null;
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

        websocketConnected = false;
        reconnectAttempts = 0;

        // Don't clear latestStatus or push null to StatusBus
        // This preserves the last known state in all UI widgets

        currentState = ConnectionState.DISCONNECTED;
        updateConnectionState(ConnectionState.DISCONNECTED);
    }

    private void updateConnectionState(ConnectionState state) {
        switch (state) {
            case DISCONNECTED:
                connectionStatusLabel.setText("OFFLINE");
                connectionStatusLabel.setStyle("-fx-text-fill: grey;");
                latestStatus = null;  // Clear cached status
                StatusBus.push(null);  // Disable all controls
                break;

            case CONNECTING:
                connectionStatusLabel.setText("CONNECTING");
                connectionStatusLabel.setStyle("-fx-text-fill: grey;");
                // Don't push null here - preserve last known status during reconnection
                // This prevents unnecessary UI flickering and console monitor restarts
                break;

            case NETWORK_ERROR:
                connectionStatusLabel.setText("NETWORK");
                connectionStatusLabel.setStyle("-fx-text-fill: #00008B;");  // Dark blue
                latestStatus = null;  // Clear cached status to prevent stale data from being pushed
                StatusBus.push(null);  // Disable all controls
                break;

            case NO_STATUS:
                connectionStatusLabel.setText("STATUS");
                connectionStatusLabel.setStyle("-fx-text-fill: red;");
                latestStatus = null;  // Clear cached status
                StatusBus.push(null);  // Disable all controls
                break;

            case CONNECTED:
                connectionStatusLabel.setText("CONNECTED");
                connectionStatusLabel.setStyle("-fx-text-fill: green;");
                // Don't push to StatusBus here - let updateWidgets do it
                break;
        }
    }

    private void updateWidgets(StatusResponse s) {
        if (s != null) {
            logger.log(Level.FINEST, "Status update: manager_state=" + s.managerState());
            StatusBus.push(s);
        }
    }
}
