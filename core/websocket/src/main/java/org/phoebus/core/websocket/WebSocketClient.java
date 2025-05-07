/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.core.websocket;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A web socket client implementation supporting pong and text messages.
 */
public class WebSocketClient implements WebSocket.Listener {

    private WebSocket webSocket;
    private final Logger logger = Logger.getLogger(WebSocketClient.class.getName());
    private Runnable connectCallback;
    private Runnable disconnectCallback;
    private final URI uri;
    private final Consumer<CharSequence> onTextCallback;
    private final AtomicBoolean attemptConnect = new AtomicBoolean(true);

    /**
     * @param uri The URI of the web socket peer.
     * @param onTextCallback A callback method the API client will use to process web socket messages.
     */
    public WebSocketClient(URI uri, Consumer<CharSequence> onTextCallback) {
        this.uri = uri;
        this.onTextCallback = onTextCallback;
    }

    /**
     * Attempts to connect to the remote web socket.
     */
    public void connect() {
        attemptConnect.set(true);
        doConnect();
    }

    /**
     * Internal connect implementation. This is done in a loop with 10 s intervals until
     * connection is established.
     */
    private void doConnect() {
        new Thread(() -> {
            while (attemptConnect.get()) {
                logger.log(Level.INFO, "Attempting web socket connection to " + uri);
                try {
                    webSocket = HttpClient.newBuilder()
                            .build()
                            .newWebSocketBuilder()
                            .buildAsync(uri, this)
                            .join();
                    break;
                } catch (Exception e) {
                    logger.log(Level.INFO, "Failed to connect to " + uri + " " + (e.getMessage() != null ? e.getMessage() : ""));
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Interrupted while sleeping");
                }
            }
        }).start();
    }

    /**
     * Called when connection has been established. An API client may optionally register a
     * {@link #connectCallback} which is called when connection is opened.
     * @param webSocket
     *         the WebSocket that has been connected
     */
    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
        if (connectCallback != null) {
            connectCallback.run();
        }
        logger.log(Level.INFO, "Connected to " + uri);
    }

    /**
     * Send a text message to peer.
     *
     * @param message The actual message. In practice a JSON formatted string that peer can evaluate
     *                to take proper action.
     */
    public void sendText(String message) {
        try {
            webSocket.sendText(message, true).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Called when connection has been closed, e.g. by remote peer. An API client may optionally register a
     * {@link #disconnectCallback} which is called when connection is opened.
     *
     * <p>
     *     Note that reconnection will be attempted immediately.
     * </p>
     * @param webSocket
     *         the WebSocket that has been connected
     */
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket,
                                      int statusCode,
                                      String reason) {
        logger.log(Level.INFO, "Web socket closed, status code=" + statusCode + ", reason: " + reason);
        if (disconnectCallback != null) {
            disconnectCallback.run();
        }
        doConnect();
        return null;
    }

    /**
     * Utility method to check connectivity. Peer should respond such that {@link #onPong(WebSocket, ByteBuffer)}
     * is called.
     */
    public void sendPing() {
        logger.log(Level.INFO, "Sending ping");
        webSocket.sendPing(ByteBuffer.allocate(0));
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        logger.log(Level.INFO, "Got pong");
        return WebSocket.Listener.super.onPong(webSocket, message);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        logger.log(Level.WARNING, "Got web socket error", error);
        WebSocket.Listener.super.onError(webSocket, error);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket,
                                     CharSequence data,
                                     boolean last) {
        webSocket.request(1);
        if (onTextCallback != null) {
            onTextCallback.accept(data);
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    /**
     *
     * <b>NOTE:</b> this <b>must</b> be called by the API client when web socket messages are no longer
     * needed, otherwise reconnect attempts will continue as these run on a separate thread.
     *
     * <p>
     *     The status code 1000 is used when calling the {@link WebSocket#sendClose(int, String)} method. See
     *     list of common web socket status codes
     *     <a href='https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent/code'>here</a>.
     * </p>
     * @param reason Custom reason text.
     */
    public void close(String reason) {
        attemptConnect.set(false);
        webSocket.sendClose(1000, reason);
    }

    /**
     * @param connectCallback A {@link Runnable} invoked when web socket connects successfully.
     */
    public void setConnectCallback(Runnable connectCallback) {
        this.connectCallback = connectCallback;
    }

    /**
     * @param disconnectCallback A {@link Runnable} invoked when web socket disconnects, either
     *                           when closed explicitly, or if remote peer goes away.
     */
    public void setDisconnectCallback(Runnable disconnectCallback) {
        this.disconnectCallback = disconnectCallback;
    }
}
