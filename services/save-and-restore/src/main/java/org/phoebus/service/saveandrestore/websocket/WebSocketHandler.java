/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 *
 */

package org.phoebus.service.saveandrestore.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PreDestroy;
import java.io.EOFException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single web socket end-point routing messages to active {@link WebSocket} instances.
 *
 * <p>
 *     In some cases web socket clients may become stale/disconnected for various reasons, e.g. network issues. The
 *     {@link #afterConnectionClosed(WebSocketSession, CloseStatus)} is not necessarily called in those case.
 *     To make sure the {@link #sockets} collection does not contain stale clients, a scheduled job runs once per hour to
 *     ping all clients, and set the time when the pong response was received. Another scheduled job will check
 *     the last received pong message timestamp and - if older than 70 minutes - consider the client session dead
 *     and dispose of it.
 * </p>
 */
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    /**
     * List of active {@link WebSocket}
     */
    @SuppressWarnings("unused")
    private List<WebSocket> sockets = Collections.synchronizedList(new ArrayList<>());

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
            Optional<WebSocket> webSocketOptional;
            synchronized (sockets){
                webSocketOptional =
                        sockets.stream().filter(webSocket -> webSocket.getId().equals(session.getId())).findFirst();
            }
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
        InetSocketAddress inetSocketAddress = session.getRemoteAddress();
        logger.log(Level.INFO, "Opening web socket session from remote " + (inetSocketAddress != null ? inetSocketAddress.getAddress().toString() : "<unknown IP address>"));
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
        Optional<WebSocket> webSocketOptional;
        synchronized (sockets){
                webSocketOptional = sockets.stream().filter(webSocket -> webSocket.getId().equals(session.getId())).findFirst();
        }
        if (webSocketOptional.isPresent()) {
            logger.log(Level.INFO, "Closing web socket session " + webSocketOptional.get().getDescription());
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
     * Called when client sends ping message, i.e. a pong message is sent and time for last pong response message
     * in the {@link WebSocket} instance is refreshed.
     *
     * @param session Associated {@link WebSocketSession}
     * @param message See {@link PongMessage}
     */
    @Override
    protected void handlePongMessage(@NonNull WebSocketSession session, @NonNull PongMessage message) {
        logger.log(Level.FINE, "Got pong for session " + session.getId());
        // Find the WebSocket instance associated with this WebSocketSession
        Optional<WebSocket> webSocketOptional;
        synchronized (sockets) {
            webSocketOptional = sockets.stream().filter(webSocket -> webSocket.getId().equals(session.getId())).findFirst();
        }
        if (webSocketOptional.isPresent()) {
            webSocketOptional.get().setLastPinged(Instant.now());
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
        synchronized (sockets) {
            sockets.forEach(s -> {
                logger.log(Level.INFO, "Disposing socket " + s.getDescription());
                s.dispose();
            });
        }
    }

    public void sendMessage(SaveAndRestoreWebSocketMessage webSocketMessage) {
        synchronized (sockets) {
            sockets.forEach(ws -> {
                try {
                    ws.queueMessage(objectMapper.writeValueAsString(webSocketMessage));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * Sends a ping message to all clients contained in {@link #sockets}.
     * <p>
     *     This is scheduled to run at the top of each hour, i.e. 00.00, 01.00...23.00
     * </p>
     *
     */
    @SuppressWarnings("unused")
    @Scheduled(cron = "* 0 * * * *")
    public void pingClients(){
        synchronized (sockets) {
            sockets.forEach(WebSocket::sendPing);
        }
    }

    /**
     * For each client in {@link #sockets}, checks the timestamp of last received pong message. If this is older
     * than 70 minutes, the socket is considered dead, and then disposed.
     * <p>
     *     This is scheduled to run 5 minutes past each hour, i.e. 00.05, 01.05...23.05
     * </p>
     *
     */
    @SuppressWarnings("unused")
    @Scheduled(cron = "* 5 * * * *")
    public void cleanUpDeadSockets(){
        List<WebSocket> deadSockets = new ArrayList<>();
        Instant now = Instant.now();
        synchronized (sockets) {
            sockets.forEach(s -> {
                Instant lastPinged = s.getLastPinged();
                if (lastPinged != null && lastPinged.isBefore(now.minus(70, ChronoUnit.MINUTES))) {
                    deadSockets.add(s);
                }
            });
            deadSockets.forEach(d -> {
                sockets.remove(d);
                d.dispose();
            });
        }
    }
}
