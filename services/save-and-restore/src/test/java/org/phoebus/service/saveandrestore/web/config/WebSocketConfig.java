/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.service.saveandrestore.websocket.WebSocket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
@ComponentScan(basePackages = {"org.phoebus.service.saveandrestore"})
@PropertySource("classpath:application.properties")
public class WebSocketConfig {

    private WebSocketConnectionManager webSocketConnectionManager;
    private TestWebSocketHandler testWebSocketHandler;

    @Bean
    public WebSocketConnectionManager webSocketConnectionManager() {
        testWebSocketHandler = new TestWebSocketHandler();
        webSocketConnectionManager =
                new WebSocketConnectionManager(new StandardWebSocketClient(),
                        testWebSocketHandler,
                        "ws://localhost:8080/web-socket",
                        new Object[]{});
        return webSocketConnectionManager;
    }

    @Bean
    public TestWebSocketHandler testWebSocketHandler() {
        return testWebSocketHandler;
    }

    public static class TestWebSocketHandler extends TextWebSocketHandler {

        private WebSocket webSocket;

        private final Logger logger = Logger.getLogger(org.phoebus.service.saveandrestore.websocket.WebSocketHandler.class.getName());
        private ObjectMapper objectMapper = new ObjectMapper();


        @Override
        public void afterConnectionEstablished(@NonNull WebSocketSession session) {
            logger.log(Level.INFO, "Opening web socket sesssion from remote " + session.getRemoteAddress().getAddress());
            webSocket = new WebSocket(objectMapper, session);
        }

        @Override
        public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
            if (webSocket != null) {
                webSocket.dispose();
            }
        }

        @Override
        public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
            try {
                if (webSocket != null) {
                    webSocket.handleTextMessage(message);
                }
            } catch (final Exception ex) {
                logger.log(Level.WARNING, ex, () -> "Error for message " + message.getPayload());
            }
        }

    }
}
