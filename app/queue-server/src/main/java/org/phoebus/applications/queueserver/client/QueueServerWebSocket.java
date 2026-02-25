package org.phoebus.applications.queueserver.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic WebSocket client for Queue Server streaming endpoints.
 * Connects to a WebSocket, parses JSON messages to the specified type, and notifies listeners.
 *
 * @param <T> the message type (e.g., ConsoleOutputWsMessage, StatusWsMessage)
 */
public final class QueueServerWebSocket<T> implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(QueueServerWebSocket.class.getPackageName());
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String wsUrl;
    private final String authHeader;
    private final Class<T> messageType;
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean active = new AtomicBoolean(false);

    private WebSocket webSocket;
    private final StringBuilder messageBuffer = new StringBuilder();

    /**
     * Create a new WebSocket client.
     *
     * @param wsUrl       the WebSocket URL (e.g., "ws://localhost:60610/api/console_output/ws")
     * @param apiKey      the API key for authentication
     * @param messageType the class of the message type to parse
     */
    public QueueServerWebSocket(String wsUrl, String apiKey, Class<T> messageType) {
        this.wsUrl = wsUrl;
        this.authHeader = "ApiKey " + apiKey;
        this.messageType = messageType;
    }

    /**
     * Add a listener that will be called when messages are received.
     *
     * @param listener the listener to add
     */
    public void addListener(Consumer<T> listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(Consumer<T> listener) {
        listeners.remove(listener);
    }

    /**
     * Connect to the WebSocket and start receiving messages.
     */
    public void connect() {
        if (active.getAndSet(true)) {
            logger.log(Level.WARNING, "WebSocket already connected: " + wsUrl);
            return;
        }

        logger.log(Level.FINE, "Connecting to WebSocket: " + wsUrl);

        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
                .header("Authorization", authHeader)
                .buildAsync(URI.create(wsUrl), new WebSocketListener());

        wsFuture.whenComplete((ws, ex) -> {
            if (ex != null) {
                logger.log(Level.FINE, "Failed to connect to WebSocket: " + wsUrl + " (" + ex.getMessage() + ")");
                active.set(false);
            } else {
                this.webSocket = ws;
                logger.log(Level.FINE, "WebSocket connected: " + wsUrl);
            }
        });
    }

    /**
     * Disconnect from the WebSocket.
     */
    public void disconnect() {
        if (!active.getAndSet(false)) {
            return;
        }

        logger.log(Level.FINE, "Disconnecting from WebSocket: " + wsUrl);

        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client closing");
            webSocket = null;
        }
    }

    /**
     * Check if the WebSocket is currently connected.
     */
    public boolean isConnected() {
        return active.get() && webSocket != null && !webSocket.isInputClosed();
    }

    @Override
    public void close() {
        disconnect();
    }

    /**
     * WebSocket listener implementation.
     */
    private class WebSocketListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            logger.log(Level.FINE, "WebSocket opened: " + wsUrl);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);

            if (last) {
                String fullMessage = messageBuffer.toString();
                messageBuffer.setLength(0);

                try {
                    T message = JSON.readValue(fullMessage, messageType);
                    notifyListeners(message);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to parse WebSocket message: " + fullMessage, e);
                }
            }

            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            // Not expected for Queue Server WebSockets
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            logger.log(Level.FINE, "WebSocket closed: " + wsUrl + " (" + statusCode + ": " + reason + ")");
            active.set(false);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            logger.log(Level.SEVERE, "WebSocket error: " + wsUrl, error);
            active.set(false);
        }
    }

    /**
     * Notify all listeners with the received message.
     */
    private void notifyListeners(T message) {
        for (Consumer<T> listener : listeners) {
            try {
                listener.accept(message);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in WebSocket listener", e);
            }
        }
    }
}
