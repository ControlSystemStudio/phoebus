/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebSocketClient implements WebSocket.Listener {

    private static WebSocketClient instance;
    private WebSocket webSocket;
    private boolean pingThreadRunning;
    private boolean connectThreadRunning;
    private CountDownLatch countDownLatch;
    private final Logger logger = Logger.getLogger(WebSocketClient.class.getName());

    public WebSocketClient() {
        connect();
    }

    public void connect() {
        new Thread(() -> {
            long waitTime = 5000;
            int connectAttempt = 0;
            connectThreadRunning = true;
            while (connectThreadRunning) {
                logger.log(Level.INFO, "Connecting to ws://localhost:8080/web-socket");
                try {
                    webSocket = HttpClient.newBuilder()
                            .build()
                            .newWebSocketBuilder()
                            .buildAsync(URI.create("ws://localhost:8080/web-socket"), this)
                            .join();
                    logger.log(Level.INFO, "Successfully connected to ws://localhost:8080/web-socket");
                    connectThreadRunning = false;
                    break;
                } catch (Exception e) {
                    logger.log(Level.INFO, "Failed to connect to ws://localhost:8080/web-socket");
                }
                try {
                    Thread.sleep(Math.round(Math.pow(2, connectAttempt++) * waitTime));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
        pingThreadRunning = true;
        startPingThread();
        logger.log(Level.INFO, "onOpen called");
    }

    public void sendText(String message) {
        try {
            webSocket.sendText(message, true).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        webSocket.sendClose(771, "Fed up");
        webSocket.abort();
    }


    @Override
    public CompletionStage<?> onClose(WebSocket webSocket,
                                      int statusCode,
                                      String reason) {
        logger.log(Level.INFO, "onClose called");
        return null;
    }

    public void sendPing() {
        webSocket.sendPing(ByteBuffer.allocate(0));
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        logger.log(Level.INFO, "Got pong ");
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
        return WebSocket.Listener.super.onPong(webSocket, message);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket,
                                       ByteBuffer data,
                                       boolean last) {
        webSocket.request(1);
        return WebSocket.Listener.super.onBinary(webSocket, data, last);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        error.printStackTrace();
        logger.log(Level.INFO, "onError called");
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket,
                                     CharSequence data,
                                     boolean last) {
        webSocket.request(1);

        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    private void startPingThread() {
        new Thread(() -> {
            while (pingThreadRunning) {
                countDownLatch = new CountDownLatch(1);
                logger.log(Level.INFO, "Sending ping");
                sendPing();
                try {
                    countDownLatch.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (countDownLatch.getCount() == 0) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }


}
