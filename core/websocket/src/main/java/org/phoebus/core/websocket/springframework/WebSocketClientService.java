/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.core.websocket.springframework;

import org.phoebus.core.websocket.WebSocketMessageHandler;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.ConnectionLostException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service wrapping a STOMP web socket client based on Spring Framework APIs.
 * Features:
 * <ul>
 *     <li>Manages keep alive as supported by the Spring Framework libs</li>
 *     <li>Passes string messages to registered {@link WebSocketMessageHandler}a</li>
 *     <li>Calls {@link Runnable}s (if specified) to signal connection or disconnection</li>
 *     <li>Attempts to auto-reconnect if web socket is closed by remote peer.</li>
 * </ul>
 * <p>
 *     All messages received from the remote peer are strings only, but may be JSON formatted.
 *     In other words, depending on the use case a client may wish to deserialize messages.
 * <p>
 *     Since web socket URL paths are currently hard coded, a remote peer (e.g. Spring Framework STOMP web socket service) must:
 *     <ul>
 *         <li>Publish a connect URL like ws(s)://host:port/path/web-socket, where path is optional.</li>
 *         <li>Publish a topic named /path/web-socket/messages, where path is optional.</li>
 *     </ul>
 * </p>
 * <p>
 *     <b>NOTE:</b> client code <i>must</i> call the {@link #shutdown()} method to not
 *     leave the web socket connection alive.
 * </p>
 */
public class WebSocketClientService {


    private StompSession stompSession;
    private Runnable connectCallback;
    private Runnable disconnectCallback;
    private final List<WebSocketMessageHandler> webSocketMessageHandlers = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean attemptReconnect = new AtomicBoolean();
    /**
     * Full path to the web socket connection URL, e.g. ws://localhost:8080/Olog/web-socket
     */
    private final String connectUrl;
    /**
     * Subscription endpoint, e.g. /Olog/web-socket/messages
     */
    private final String subscriptionEndpoint;
    /**
     * Echo endpoint /Olog/web-socket/echo
     */
    private final String echoEndpoint;

    private static final Logger logger = Logger.getLogger(WebSocketClientService.class.getName());

    /**
     * Constructor if connect/disconnect  callbacks are not needed.
     */
    @SuppressWarnings("unused")
    public WebSocketClientService(String connectUrl, String subscriptionEndpoint, String echoEndpoint) {
        this.connectUrl = connectUrl;
        this.subscriptionEndpoint = subscriptionEndpoint;
        this.echoEndpoint = echoEndpoint;
    }

    /**
     * @param connectCallback      The non-null method called when connection to the remote web socket has been successfully established.
     * @param disconnectCallback   The non-null method called when connection to the remote web socket has been lost, e.g.
     *                             remote peer has been shut down.
     * @param connectUrl           URL to the service web socket, e.g. ws://localhost:8080/Olog/web.socket
     * @param subscriptionEndpoint E.g. /Olog/web-socket/messages
     * @param echoEndpoint         E.g. /Olog/web-socket/echo. May be <code>null</code> if client has no need for echo messages.
     */
    public WebSocketClientService(Runnable connectCallback, Runnable disconnectCallback, String connectUrl, String subscriptionEndpoint, String echoEndpoint) {
        this(connectUrl, subscriptionEndpoint, echoEndpoint);
        this.connectCallback = connectCallback;
        this.disconnectCallback = disconnectCallback;
    }

    @SuppressWarnings("unused")
    public void setConnectCallback(Runnable connectCallback) {
        this.connectCallback = connectCallback;
    }

    @SuppressWarnings("unused")
    public void setDisconnectCallback(Runnable disconnectCallback) {
        this.disconnectCallback = disconnectCallback;
    }

    public void addWebSocketMessageHandler(WebSocketMessageHandler webSocketMessageHandler) {
        webSocketMessageHandlers.add(webSocketMessageHandler);
    }

    public void removeWebSocketMessageHandler(WebSocketMessageHandler webSocketMessageHandler) {
        webSocketMessageHandlers.remove(webSocketMessageHandler);
    }

    /**
     * For debugging purposes: peer should just echo back the message on the subscribed topic.
     *
     * @param message Message for the service to echo
     */
    @SuppressWarnings("unused")
    public void sendEcho(String message) {
        if (stompSession != null && stompSession.isConnected() && echoEndpoint != null) {
            stompSession.send(echoEndpoint, message);
        }
    }

    /**
     * Disconnects the socket if connected and terminates connection thread.
     */
    public synchronized void shutdown() {
        attemptReconnect.set(false);
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }

    /**
     * Attempts to connect to the remote peer, both in initial connection and in a reconnection scenario.
     * If connection fails, new attempts are made every 10s until successful.
     */
    public void connect() {
        attemptReconnect.set(true);
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new StringMessageConverter());
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();
        stompClient.setTaskScheduler(threadPoolTaskScheduler);
        stompClient.setDefaultHeartbeat(new long[]{30000, 30000});
        StompSessionHandler sessionHandler = new StompSessionHandler();
        logger.log(Level.INFO, "Attempting web socket connection to " + connectUrl);
        new Thread(() -> {
            while (true) {
                try{
                    synchronized (WebSocketClientService.this) {
                        if(attemptReconnect.get()) {
                            stompSession = stompClient.connect(connectUrl, sessionHandler).get();
                            stompSession.subscribe(this.subscriptionEndpoint, new StompFrameHandler() {
                                @Override
                                public Type getPayloadType(StompHeaders headers) {
                                    return String.class;
                                }

                                @Override
                                public void handleFrame(StompHeaders headers, Object payload) {
                                    logger.log(Level.INFO, "Handling subscription frame: " + payload);
                                    webSocketMessageHandlers.forEach(h -> h.handleWebSocketMessage((String) payload));
                                }
                            });
                            attemptReconnect.set(false);
                        }
                        break;
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Got exception when trying to connect", e);
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Got exception when trying to connect", e);
                }
            }
        }).start();
    }

    /**
     * Handler used to perform housekeeping, e.g. trigger reconnection attempts if connection goes down.
     */
    private class StompSessionHandler extends StompSessionHandlerAdapter {

        /**
         * Logs that web socket frame has been received.
         *
         * @param headers the headers of the frame
         * @param payload the payload, or {@code null} if there was no payload
         */
        @Override
        public void handleFrame(StompHeaders headers, @Nullable Object payload) {
            if (payload != null) {
                logger.log(Level.FINE, "WebSocket frame received: " + payload);
            }
        }

        /**
         * Handles connection success callback: thread to attempt connection is aborted,
         * and connect callback is called, if set by API client.
         *
         * @param session          the client STOMP session
         * @param connectedHeaders the STOMP CONNECTED frame headers
         */
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            logger.log(Level.INFO, "Connected to web socket");
            if (connectCallback != null) {
                connectCallback.run();
            }
        }

        /**
         * Hit for instance if an attempt is made to send a message to peer after {@link StompSession} has been closed.
         *
         * @param session   the client STOMP session
         * @param command   the STOMP command of the frame
         * @param headers   the headers
         * @param payload   the raw payload
         * @param exception the exception
         */
        @Override
        public void handleException(StompSession session, @Nullable StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            logger.log(Level.WARNING, "Exception encountered", exception);
        }

        /**
         * If remote peer goes away because the service is shut down, or because
         * of a network connection issue, we get a {@link ConnectionLostException}. In this case
         * a reconnection thread is started. If on the other hand a connection attempt fails, we get
         * a different type of exception (javax.websocket.DeploymentException), in which case a
         * reconnection thread is not started.
         *
         * @param session   the client STOMP session
         * @param exception the exception that occurred. This is evaluated to determine if a reconnection
         *                  thread should be launched.
         */
        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            if (exception instanceof ConnectionLostException) {
                logger.log(Level.WARNING, "Connection lost, will attempt to reconnect", exception);
                connect();
            } else {
                logger.log(Level.WARNING, "Handling transport exception", exception);
            }
            if (disconnectCallback != null) {
                disconnectCallback.run();
            }
        }
    }

    /**
     * Utility method to check availability of a web socket connection. Tries to connect once,
     * and - if successful - subsequently closes the web socket connection.
     *
     * @param webSocketConnectUrl The web socket URL
     * @return <code>true</code> if connection to web socket succeeds within 3000 ms.
     */
    public static boolean checkAvailability(String webSocketConnectUrl) {
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        try {
            StompSession stompSession = stompClient.connect(webSocketConnectUrl, new StompSessionHandlerAdapter() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return super.getPayloadType(headers);
                }
            }).get(3000, TimeUnit.MILLISECONDS);
            stompSession.disconnect();
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Remote service on " + webSocketConnectUrl + " does not support web socket connection", e);
        }
        return false;
    }
}
