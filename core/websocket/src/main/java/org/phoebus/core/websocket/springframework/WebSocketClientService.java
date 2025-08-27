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
import java.net.URI;
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
 */
public class WebSocketClientService {

    /**
     * URL to which the client connects
     */
    private String webSocketConnectUrl;
    /**
     * Path string, depends on service deployment context, e.g. Olog
     */
    private String contextPath;
    private StompSession stompSession;
    private Runnable connectCallback;
    private Runnable disconnectCallback;
    private final List<WebSocketMessageHandler> webSocketMessageHandlers = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean attemptReconnect = new AtomicBoolean();

    private static final Logger logger = Logger.getLogger(WebSocketClientService.class.getName());

    public WebSocketClientService() {

    }

    /**
     * @param connectCallback    The non-null method called when connection to the remote web socket has been successfully established.
     * @param disconnectCallback The non-null method called when connection to the remote web socket has been lost, e.g.
     *                           remote peer has been shut down.
     */
    public WebSocketClientService(Runnable connectCallback, Runnable disconnectCallback) {
        this.connectCallback = connectCallback;
        this.disconnectCallback = disconnectCallback;
    }

    public void setConnectCallback(Runnable connectCallback) {
        this.connectCallback = connectCallback;
    }

    public void setDisconnectCallback(Runnable disconnectCallback) {
        this.disconnectCallback = disconnectCallback;
    }

    public void addWebSocketMessageHandler(WebSocketMessageHandler webSocketMessageHandler) {
        webSocketMessageHandlers.add(webSocketMessageHandler);
    }

    public void removeWebSocketMessageHandler(WebSocketMessageHandler webSocketMessageHandler) {
        webSocketMessageHandlers.remove(webSocketMessageHandler);
    }


    public void sendEcho(String message) {
        stompSession.send(contextPath + "/web-socket/echo", message);
    }

    public void close(){
        stompSession.disconnect();
    }

    /**
     * Attempts to connect to remote web socket.
     *
     * @param baseUrl The &quot;base&quot; URL of the web socket peer, must start with ws:// or wss://. Note that &quot;web-socket&quot; will be
     *                appended to this URL. Further, the URL may contain a path, e.g. ws://host:port/path.
     * @throws IllegalArgumentException if <code>baseUrl</code> is null, empty or does not start with ws:// or wss://.
     */
    public void connect(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty() || (!baseUrl.toLowerCase().startsWith("ws://") && !baseUrl.toLowerCase().startsWith("wss://"))) {
            throw new IllegalArgumentException("URL \"" + baseUrl + "\" is not valid");
        }
        URI uri = URI.create(baseUrl);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        this.contextPath = path;
        this.webSocketConnectUrl = scheme + "://" + host + (port > -1 ? (":" + port) : "") + this.contextPath + "/web-socket";
        doConnect();
    }

    private void doConnect() {
        attemptReconnect.set(true);
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new StringMessageConverter());
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();
        stompClient.setTaskScheduler(threadPoolTaskScheduler);
        stompClient.setDefaultHeartbeat(new long[]{30000, 30000});
        StompSessionHandler sessionHandler = new StompSessionHandler();
        new Thread(() -> {
            while (attemptReconnect.get()) {
                logger.log(Level.INFO, "Attempting web socket connection to " + webSocketConnectUrl);
                try {
                    stompSession = stompClient.connect(this.webSocketConnectUrl, sessionHandler).get();
                    stompSession.subscribe(contextPath + "/web-socket/messages", new StompFrameHandler() {
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

        @Override
        public void handleFrame(StompHeaders headers, @Nullable Object payload) {
            if (payload != null) {
                logger.log(Level.INFO, "WebSocket frame received: " + payload);
            }
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            attemptReconnect.set(false);
            logger.log(Level.INFO, "Connected to web socket");
            if (connectCallback != null) {
                connectCallback.run();
            }
        }

        @Override
        public void handleException(StompSession session, @Nullable StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            logger.log(Level.WARNING, "Exception encountered", exception);
        }

        /**
         * Note that this is called both on connection failure and if remote web socket peer
         * goes away for whatever reason.
         * @param session the client STOMP session
         * @param exception the exception that occurred. This is evaluated to determine if a reconnection
         *                  thread should be launched.
         */
        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            if(exception instanceof ConnectionLostException){
                logger.log(Level.WARNING, "Connection lost, will attempt to reconnect", exception);
                doConnect();
            }
            else{
                logger.log(Level.WARNING, "Handling transport exception", exception);
            }
            if (disconnectCallback != null) {
                disconnectCallback.run();
            }
        }
    }
}
