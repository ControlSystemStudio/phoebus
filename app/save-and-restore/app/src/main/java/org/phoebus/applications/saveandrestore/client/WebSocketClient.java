/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.ExecutionException;

public class WebSocketClient implements WebSocket.Listener {


    public WebSocketClient(URI endpointURI) {
        try {
            WebSocket webSocket =
                    HttpClient.newHttpClient().newWebSocketBuilder()
                            .buildAsync(endpointURI, this).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

    }
}
