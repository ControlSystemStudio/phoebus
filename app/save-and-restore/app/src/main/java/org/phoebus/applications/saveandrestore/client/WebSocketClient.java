/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class WebSocketClient implements WebSocket.Listener {

    private static WebSocketClient instance;

    public static WebSocketClient getInstance(){
        if(instance == null){
            instance = new WebSocketClient();
        }
        return instance;
    }

    private WebSocketClient(){
        try {
            WebSocket webSocket = HttpClient.newBuilder()
                    .build()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:8080/web-socket"), this).get();
            //webSocket.sendText("Wake Up", true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket){
        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        byteBuffer.put("Hello".getBytes());
        try {
            webSocket.sendText("{'a':771}", true).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


}
