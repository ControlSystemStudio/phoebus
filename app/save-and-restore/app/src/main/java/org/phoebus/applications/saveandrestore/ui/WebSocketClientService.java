/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.phoebus.applications.saveandrestore.client.Preferences;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.applications.saveandrestore.model.websocket.WebMessageDeserializer;
import org.phoebus.core.websocket.WebSocketClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WebSocketClientService {

    private final List<WebSocketMessageHandler> webSocketMessageHandlers = Collections.synchronizedList(new ArrayList<>());

    private static WebSocketClientService instance;
    private final WebSocketClient webSocketClient;

    private final ObjectMapper objectMapper;

    private WebSocketClientService() {
        String baseUrl = Preferences.jmasarServiceUrl;
        String schema = baseUrl.startsWith("https") ? "wss" : "ws";
        String webSocketUrl = schema + baseUrl.substring(baseUrl.indexOf("://")) + "/web-socket";
        URI webSocketUri = URI.create(webSocketUrl);
        webSocketClient = new WebSocketClient(webSocketUri, this::handleWebSocketMessage);
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(SaveAndRestoreWebSocketMessage.class,
                new WebMessageDeserializer(SaveAndRestoreWebSocketMessage.class));
        objectMapper.registerModule(module);
    }

    public static WebSocketClientService getInstance() {
        if (instance == null) {
            instance = new WebSocketClientService();
        }
        return instance;
    }

    public void connect() {
        webSocketClient.connect();
    }

    public void closeWebSocket() {
        webSocketMessageHandlers.clear();
        webSocketClient.close("Application shutdown");
    }

    public void addWebSocketMessageHandler(WebSocketMessageHandler webSocketMessageHandler) {
        webSocketMessageHandlers.add(webSocketMessageHandler);
    }

    public void removeWebSocketMessageHandler(WebSocketMessageHandler webSocketMessageHandler) {
        webSocketMessageHandlers.remove(webSocketMessageHandler);
    }

    private void handleWebSocketMessage(CharSequence charSequence) {
        try {
            SaveAndRestoreWebSocketMessage saveAndRestoreWebSocketMessage =
                    objectMapper.readValue(charSequence.toString(), SaveAndRestoreWebSocketMessage.class);
            webSocketMessageHandlers.forEach(w -> w.handleWebSocketMessage(saveAndRestoreWebSocketMessage));

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setConnectCallback(Runnable connectCallback) {
        webSocketClient.setConnectCallback(connectCallback);
    }

    public void setDisconnectCallback(Runnable disconnectCallback) {
        webSocketClient.setDisconnectCallback(disconnectCallback);
    }
}
