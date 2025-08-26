/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.core.websocket.springframework;

import org.phoebus.core.websocket.WebSocketMessageHandler;
import org.phoebus.framework.jobs.JobManager;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.StringMessageConverter;
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

    private String baseUrl;
    private StompSession stompSession;
    private Runnable connectCallback;
    private Runnable disconnectCallback;
    private final List<WebSocketMessageHandler> webSocketMessageHandlers = Collections.synchronizedList(new ArrayList<>());

    private static final Logger logger = Logger.getLogger(WebSocketClientService.class.getName());

    public WebSocketClientService(){

    }

    /**
     * @param connectCallback The non-null method called when connection to the remote web socket has been successfully established.
     * @param disconnectCallback The non-null method called when connection to the remote web socket has been lost, e.g.
     *                           remote peer has been shut down.
     */
    public WebSocketClientService(Runnable connectCallback, Runnable disconnectCallback) {
        this.connectCallback = connectCallback;
        this.disconnectCallback = disconnectCallback;
    }

    public void setConnectCallback(Runnable connectCallback){
        this.connectCallback = connectCallback;
    }

    public void setDisconnectCallback(Runnable disconnectCallback){
        this.disconnectCallback = disconnectCallback;
    }

    public void addWebSocketMessageHandler(WebSocketMessageHandler webSocketMessageHandler) {
        webSocketMessageHandlers.add(webSocketMessageHandler);
    }

    public void removeWebSocketMessageHandler(WebSocketMessageHandler webSocketMessageHandler) {
        webSocketMessageHandlers.remove(webSocketMessageHandler);
    }

    private void connect(){
        connect(baseUrl);
    }

    /**
     * Attempts to connect to remote web socket.
     * @param baseUrl The &quot;base&quot; URL of the web socket peer, must start with ws:// or wss://. Note that &quot;web-socket&quot; will be
     *                appended to this URL. Further, the URL may contain a path, e.g. ws://host:port/path.
     * @throws IllegalArgumentException if <code>baseUrl</code> is null, empty or does not start with ws:// or wss://.
     */
    public void connect(String baseUrl) {
        if(baseUrl == null || baseUrl.isEmpty() || (!baseUrl.toLowerCase().startsWith("ws://") && !baseUrl.toLowerCase().startsWith("wss://"))){
            throw new IllegalArgumentException("URL \"" + baseUrl + "\" is not valid");
        }
        this.baseUrl = baseUrl;
        URI uri = URI.create(baseUrl);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new StringMessageConverter());
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();
        stompClient.setTaskScheduler(threadPoolTaskScheduler);
        stompClient.setDefaultHeartbeat(new long[]{30000, 30000});
        StompSessionHandler sessionHandler = new StompSessionHandler();
        String _path = path;
        String webSocketUrl = scheme + "://" + host + (port > -1 ? (":" + port) : "") + "/web-socket";
        JobManager.schedule("Connect to web socket", monitor -> {
            stompSession = stompClient.connect(webSocketUrl, sessionHandler).get();
            logger.log(Level.INFO, "Subscribing to messages on " + _path + "/web-socket/messages");
            stompSession.subscribe(_path + "/web-socket/messages", new StompFrameHandler() {
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
        });
    }

    /**
     * Handler used to perform housekeeping...
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

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            logger.log(Level.WARNING, "Handling web socket transport error: " + exception.getMessage(), exception);
            if (disconnectCallback != null) {
                disconnectCallback.run();
            }
        }
    }
}
