/*
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.service.saveandrestore.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PreDestroy;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single web socket end-point routing messages to active {@link WebSocket} instances.
 */
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    /**
     * List of active {@link WebSocket}
     */
    @SuppressWarnings("unused")
    private List<WebSocket> sockets = new ArrayList<>();

    @SuppressWarnings("unused")
    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("unused")

    private final Logger logger = Logger.getLogger(WebSocketHandler.class.getName());

    /**
     * Handles text message from web socket client
     *
     * @param session The {@link WebSocketSession} associated with the remote client.
     * @param message Message sent by client
     */
    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        try {
            // Find the WebSocket instance associated with the WebSocketSession
            Optional<WebSocket> webSocketOptional =
                    sockets.stream().filter(webSocket -> webSocket.getId().equals(session.getId())).findFirst();
            if (webSocketOptional.isEmpty()) {
                return; // Should only happen in case of timing issues?
            }
            webSocketOptional.get().handleTextMessage(message);
        } catch (final Exception ex) {
            logger.log(Level.WARNING, ex, () -> "Error for message " + shorten(message.getPayload()));
        }
    }

    /**
     * Called when client connects.
     *
     * @param session Associated {@link WebSocketSession}
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        logger.log(Level.INFO, "Opening web socket session from remote " + session.getRemoteAddress().getAddress());
        WebSocket webSocket = new WebSocket(objectMapper, session);
        sockets.add(webSocket);
    }

    /**
     * Called when web socket is closed. Depending on the web browser, {@link #handleTransportError(WebSocketSession, Throwable)}
     * may be called first.
     *
     * @param session Associated {@link WebSocketSession}
     * @param status  See {@link CloseStatus}
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        Optional<WebSocket> webSocketOptional =
                sockets.stream().filter(webSocket -> webSocket.getId().equals(session.getId())).findFirst();
        if (webSocketOptional.isPresent()) {
            logger.log(Level.INFO, "Closing web socket session from remote " + session.getRemoteAddress().getAddress());
            webSocketOptional.get().dispose();
            sockets.remove(webSocketOptional.get());
        }
    }

    /**
     * Depending on the web browser, this is called before {@link #afterConnectionClosed(WebSocketSession, CloseStatus)}
     * when tab or browser is closes.
     *
     * @param session Associated {@link WebSocketSession}
     * @param ex      {@link Throwable} that should indicate reason
     */
    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable ex) {
        if (ex instanceof EOFException)
            logger.log(Level.FINE, "Web Socket closed", ex);
        else
            logger.log(Level.WARNING, "Web Socket error", ex);
    }

    /**
     * Called when client sends ping message, i.e. a pong message is sent and time for last message
     * in the {@link WebSocket} instance is refreshed.
     *
     * @param session Associated {@link WebSocketSession}
     * @param message See {@link PongMessage}
     */
    @Override
    protected void handlePongMessage(@NonNull WebSocketSession session, @NonNull PongMessage message) {
        logger.log(Level.INFO, "Got pong");
        // Find the WebSocket instance associated with this WebSocketSession
        Optional<WebSocket> webSocketOptional =
                sockets.stream().filter(webSocket -> webSocket.getId().equals(session.getId())).findFirst();
        if (webSocketOptional.isEmpty()) {
            return; // Should only happen in case of timing issues?
        }
    }

    /**
     * @param message Potentially long message
     * @return Message shorted to 200 chars
     */
    private String shorten(final String message) {
        if (message == null || message.length() < 200)
            return message;
        return message.substring(0, 200) + " ...";
    }

    @PreDestroy
    public void cleanup() {
        sockets.forEach(s -> {
            logger.log(Level.INFO, "Disposing socket " + s.getId());
            s.dispose();
        });
    }

    public void sendMessage(SaveAndRestoreWebSocketMessage webSocketMessage) {
        sockets.forEach(ws -> {
            try {
                ws.queueMessage(objectMapper.writeValueAsString(webSocketMessage));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
