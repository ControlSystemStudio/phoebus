/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.core.websocket;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
    private URI uri;
    private Consumer<CharSequence> onTextCallback;
    private AtomicBoolean attemptConnect = new AtomicBoolean(true);

    /**
     * @param uri The URI of the web socket peer.
     */
    public WebSocketClient(URI uri, Consumer<CharSequence> onTextCallback) {
        this.uri = uri;
        this.onTextCallback = onTextCallback;
    }

    public void connect() {
        new Thread(() -> {
            while(attemptConnect.get()){
                logger.log(Level.INFO, "Attempting web socket connection to " + uri);
                try {
                    webSocket = HttpClient.newBuilder()
                            .build()
                            .newWebSocketBuilder()
                            .buildAsync(uri, this)
                            .join();
                    break;
                }
                catch (Exception e) {
                    logger.log(Level.INFO, "Failed to connect to " + uri + " " + (e != null ? e.getMessage() : ""));
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Interrupted while sleeping");
                }
            }
        }).start();
    }

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


    @Override
    public CompletionStage<?> onClose(WebSocket webSocket,
                                      int statusCode,
                                      String reason) {
        logger.log(Level.INFO, "Web socket closed, status code=" + statusCode + ", reason: " + reason);
        if (disconnectCallback != null) {
            disconnectCallback.run();
        }
        connect();
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
        logger.log(Level.WARNING, "Got web socket error");
        error.printStackTrace();
        WebSocket.Listener.super.onError(webSocket, error);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket,
                                     CharSequence data,
                                     boolean last) {
        webSocket.request(1);
        if(onTextCallback != null) {
            onTextCallback.accept(data);
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    public void close(String reason) {
        attemptConnect.set(false);
        webSocket.sendClose(1000, reason);
    }

    public void setConnectCallback(Runnable connectCallback){
        this.connectCallback = connectCallback;
    }

    public void setDisconnectCallback(Runnable disconnectCallback){
        this.disconnectCallback = disconnectCallback;
    }
}
