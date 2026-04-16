package org.phoebus.applications.queueserver.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.phoebus.applications.queueserver.Preferences;
import org.phoebus.applications.queueserver.api.StatusResponse;
import org.phoebus.applications.queueserver.api.StatusWsMessage;
import org.phoebus.applications.queueserver.client.QueueServerWebSocket;
import org.phoebus.applications.queueserver.client.RunEngineService;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton that owns the queue-server connection (WebSocket or HTTP polling).
 * Multiple UI controllers ({@link org.phoebus.applications.queueserver.controller.ReManagerConnectionController})
 * bind to the shared {@link #stateProperty()} and call {@link #start()}/{@link #stop()}.
 */
public final class ConnectionManager {

    private static final ConnectionManager INSTANCE = new ConnectionManager();
    private static final Logger logger = Logger.getLogger(ConnectionManager.class.getPackageName());

    private final RunEngineService svc = new RunEngineService();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ScheduledFuture<?> pollTask;
    private ScheduledFuture<?> reconnectTask;
    private ScheduledFuture<?> healthCheckTask;
    private QueueServerWebSocket<StatusWsMessage> statusWs;

    private volatile StatusResponse latestStatus = null;
    private volatile boolean websocketConnected = false;
    private volatile boolean attemptingConnection = false;
    private volatile long lastStatusUpdateTime = 0;
    private volatile long connectionAttemptStartTime = 0;
    private volatile int reconnectAttempts = 0;

    private static final long STATUS_TIMEOUT_MS = 3000;
    private static final long CONNECTION_ATTEMPT_TIMEOUT_MS = 5000;
    private static final long RECONNECT_INTERVAL_MS = 5000;

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        NETWORK_ERROR,
        NO_STATUS,
        CONNECTED
    }

    /** Observable state – always updated on the FX thread. */
    private final ObjectProperty<ConnectionState> state =
            new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);

    private volatile boolean autoConnect = false;

    private ConnectionManager() {}

    public static ConnectionManager getInstance() { return INSTANCE; }

    public ObjectProperty<ConnectionState> stateProperty() { return state; }

    public ConnectionState getState() { return state.get(); }

    public boolean isAutoConnect() { return autoConnect; }

    /** Idempotent – does nothing if already running. */
    public void start() {
        if (autoConnect) return;
        autoConnect = true;

        if (pollTask != null && !pollTask.isDone()) return;
        if (statusWs != null && statusWs.isConnected()) return;

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
            logger.log(Level.INFO, "Starting status polling every " + Preferences.update_interval_ms + " ms");
            attemptingConnection = true;
            connectionAttemptStartTime = System.currentTimeMillis();
            pollTask = PollCenter.everyMs(Preferences.update_interval_ms,
                    this::queryStatusOnce,
                    this::updateWidgets);
        }

        if (healthCheckTask == null) {
            healthCheckTask = PollCenter.everyMs(500, this::checkConnectionHealth);
        }
    }

    /** Idempotent – does nothing if already stopped. */
    public void stop() {
        logger.log(Level.INFO, "Stopping status monitoring");
        autoConnect = false;

        if (reconnectTask != null) { reconnectTask.cancel(true); reconnectTask = null; }
        if (healthCheckTask != null) { healthCheckTask.cancel(true); healthCheckTask = null; }
        if (pollTask != null) { pollTask.cancel(true); pollTask = null; }
        if (statusWs != null) { statusWs.disconnect(); statusWs = null; }

        websocketConnected = false;
        reconnectAttempts = 0;

        Platform.runLater(() -> {
            state.set(ConnectionState.DISCONNECTED);
            latestStatus = null;
            StatusBus.push(null);
        });
    }

    /** Full shutdown – resets everything. Called by AppLifecycle. */
    public void shutdown() {
        stop();
    }

    // ---- WebSocket connection -------------------------------------------

    private void attemptWebSocketConnection() {
        try {
            attemptingConnection = true;
            connectionAttemptStartTime = System.currentTimeMillis();

            statusWs = svc.createStatusWebSocket();
            statusWs.addListener(msg -> {
                Map<String, Object> statusMap = msg.status();
                if (statusMap != null) {
                    try {
                        latestStatus = mapper.convertValue(statusMap, StatusResponse.class);
                        lastStatusUpdateTime = System.currentTimeMillis();
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

            pollTask = PollCenter.everyMs(Preferences.update_interval_ms, () -> {
                if (statusWs != null && !statusWs.isConnected()) {
                    if (websocketConnected) {
                        logger.log(Level.WARNING, "WebSocket connection lost");
                        websocketConnected = false;
                        attemptingConnection = false;
                        if (autoConnect) scheduleReconnect();
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
            if (autoConnect) scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!autoConnect) return;
        if (reconnectTask != null && !reconnectTask.isDone()) return;
        if (attemptingConnection) return;
        if (reconnectTask != null) reconnectTask.cancel(false);

        reconnectAttempts++;
        logger.log(Level.INFO, "Scheduling reconnect attempt #" + reconnectAttempts
                + " in " + RECONNECT_INTERVAL_MS + "ms");

        reconnectTask = PollCenter.afterMs((int) RECONNECT_INTERVAL_MS, () -> {
            if (autoConnect && !attemptingConnection) {
                logger.log(Level.INFO, "Attempting reconnect (attempt #" + reconnectAttempts + ")");
                if (statusWs != null) { statusWs.disconnect(); statusWs = null; }
                if (pollTask != null) { pollTask.cancel(false); pollTask = null; }
                websocketConnected = false;
                attemptWebSocketConnection();
            }
        });
    }

    // ---- HTTP polling ---------------------------------------------------

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

    // ---- Health check ---------------------------------------------------

    private void checkConnectionHealth() {
        ConnectionState newState;

        if (!autoConnect) {
            newState = ConnectionState.DISCONNECTED;
        } else if (Preferences.use_websockets) {
            newState = checkWebSocketHealth();
        } else {
            newState = checkPollingHealth();
        }

        if (newState != state.get()) {
            ConnectionState oldState = state.get();
            logger.log(Level.FINE, "Connection state: " + oldState + " -> " + newState);
            Platform.runLater(() -> {
                state.set(newState);
                applyStateEffects(newState);
            });
        }
    }

    private ConnectionState checkWebSocketHealth() {
        if (attemptingConnection) {
            long elapsed = System.currentTimeMillis() - connectionAttemptStartTime;
            if (elapsed > CONNECTION_ATTEMPT_TIMEOUT_MS) {
                attemptingConnection = false;
                if (pollTask != null) { pollTask.cancel(false); pollTask = null; }
                if (statusWs != null) { statusWs.disconnect(); statusWs = null; }
                scheduleReconnect();
                return ConnectionState.NETWORK_ERROR;
            }
            return ConnectionState.CONNECTING;
        }
        if (!websocketConnected) {
            if ((reconnectTask == null || reconnectTask.isDone())
                    && (statusWs == null || !statusWs.isConnected())) {
                scheduleReconnect();
            }
            return ConnectionState.NETWORK_ERROR;
        }
        if (lastStatusUpdateTime == 0) {
            return ConnectionState.CONNECTING;
        }
        long age = System.currentTimeMillis() - lastStatusUpdateTime;
        return age > STATUS_TIMEOUT_MS ? ConnectionState.NO_STATUS : ConnectionState.CONNECTED;
    }

    private ConnectionState checkPollingHealth() {
        if (attemptingConnection) {
            long elapsed = System.currentTimeMillis() - connectionAttemptStartTime;
            return elapsed > CONNECTION_ATTEMPT_TIMEOUT_MS
                    ? ConnectionState.NETWORK_ERROR
                    : ConnectionState.CONNECTING;
        }
        if (lastStatusUpdateTime == 0) return ConnectionState.NETWORK_ERROR;
        long age = System.currentTimeMillis() - lastStatusUpdateTime;
        return age > STATUS_TIMEOUT_MS ? ConnectionState.NETWORK_ERROR : ConnectionState.CONNECTED;
    }

    private void applyStateEffects(ConnectionState s) {
        switch (s) {
            case DISCONNECTED, NETWORK_ERROR, NO_STATUS -> {
                latestStatus = null;
                StatusBus.push(null);
            }
            case CONNECTED -> PlansCache.loadIfNeeded();
            default -> { }
        }
    }

    // ---- Push to StatusBus ----------------------------------------------

    private void updateWidgets(StatusResponse s) {
        if (s != null) {
            logger.log(Level.FINEST, "Status update: manager_state=" + s.managerState());
            StatusBus.push(s);
        }
    }
}
