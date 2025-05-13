/*******************************************************************************
 * Copyright (c) 2019-2022 UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the LICENSE
 * which accompanies this distribution
 ******************************************************************************/
package org.phoebus.service.saveandrestore.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for handling web socket messages. In the context of the save-and-restore service,
 * only messages from server are expected. Client messages are logged, but do not invoke any behavior.
 */
@SuppressWarnings("nls")
public class WebSocket {

    /**
     * Is the queue full?
     */
    private final AtomicBoolean stuffed = new AtomicBoolean();

    /**
     * Queue of messages for the client.
     *
     * <p>Multiple threads concurrently writing to the socket results in
     * IllegalStateException "remote endpoint was in state [TEXT_FULL_WRITING]"
     * All writes are thus performed by just one thread off this queue.
     */
    private final ArrayBlockingQueue<String> writeQueue = new ArrayBlockingQueue<>(2048);

    private static final String EXIT_MESSAGE = "EXIT";

    private final WebSocketSession session;
    private final String id;

    private final Logger logger = Logger.getLogger(WebSocket.class.getName());

    private final ObjectMapper objectMapper;

    /**
     * Constructor
     */
    public WebSocket(ObjectMapper objectMapper, WebSocketSession webSocketSession) {
        this.session = webSocketSession;
        logger.log(Level.INFO, () -> "Creating web socket " + session.getUri() + " ID " + session.getId());
        this.objectMapper = objectMapper;
        this.id = webSocketSession.getId();
        Thread writeThread = new Thread(this::writeQueuedMessages, "Web Socket Write Thread");
        writeThread.setName("Web Socket Write Thread " + this.id);
        writeThread.setDaemon(true);
        writeThread.start();
    }

    /**
     * @return Session ID
     */
    public String getId() {
        if (session == null)
            return "(" + id + ")";
        else
            return id;
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

    public void queueMessage(final String message) {
        // Ignore messages after 'dispose'
        if (session == null)
            return;

        if (writeQueue.offer(message)) {   // Queued OK. Is this a recovery from stuffed queue?
            if (stuffed.getAndSet(false))
                logger.log(Level.WARNING, () -> "Un-stuffed message queue for " + id);
        } else {   // Log, but only for the first message to prevent flooding the log
            if (!stuffed.getAndSet(true))
                logger.log(Level.WARNING, () -> "Cannot queue message '" + shorten(message) + "' for " + id);
        }
    }

    private void writeQueuedMessages() {
        try {
            while (true) {
                final String message;
                try {
                    message = writeQueue.take();
                } catch (final InterruptedException ex) {
                    return;
                }

                // Check if we should exit the thread
                if (message.equals(EXIT_MESSAGE)) {
                    logger.log(Level.FINE, () -> "Exiting write thread " + id);
                    return;
                }

                final WebSocketSession safeSession = session;
                try {
                    if (safeSession == null)
                        throw new Exception("No session");
                    if (!safeSession.isOpen())
                        throw new Exception("Session closed");
                    safeSession.sendMessage(new TextMessage(message));
                } catch (final Exception ex) {
                    logger.log(Level.WARNING, ex, () -> "Cannot write '" + shorten(message) + "' for " + id);

                    // Clear queue
                    String drop = writeQueue.take();
                    while (drop != null) {
                        if (drop.equals(EXIT_MESSAGE)) {
                            logger.log(Level.FINE, () -> "Exiting write thread " + id);
                            return;
                        }
                        drop = writeQueue.take();
                    }
                }
            }
        } catch (Throwable ex) {
            logger.log(Level.WARNING, "Write thread error for " + id, ex);
        }
    }

    /**
     * Called when client sends a general message
     *
     * @param message {@link TextMessage}, its payload is expected to be JSON.
     */
    public void handleTextMessage(TextMessage message) throws Exception {
        final JsonNode json = objectMapper.readTree(message.getPayload());
        final JsonNode node = json.path("type");
        if (node.isMissingNode())
            throw new Exception("Missing 'type' in " + shorten(message.getPayload()));
        final String type = node.asText();
        logger.log(Level.INFO, "Client message type: " + type);
    }

    /**
     * Clears all PVs
     *
     * <p>Web socket calls this onClose(),
     * but context may also call this again just in case
     */
    public void dispose() {
        // Exit write thread
        try {
            // Drop queued messages (which might be stuffed):
            // We're closing and just need the EXIT_MESSAGE
            writeQueue.clear();
            queueMessage(EXIT_MESSAGE);
            // TODO: is this needed?
            session.close();
        } catch (Throwable ex) {
            logger.log(Level.WARNING, "Error disposing " + getId(), ex);
        }
        logger.log(Level.INFO, () -> "Web socket " + session.getId() + " closed");
    }
}
