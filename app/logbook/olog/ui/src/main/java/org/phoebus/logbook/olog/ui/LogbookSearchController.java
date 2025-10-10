package org.phoebus.logbook.olog.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.phoebus.core.websocket.WebSocketMessageHandler;
import org.phoebus.core.websocket.springframework.WebSocketClientService;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.SearchResult;
import org.phoebus.logbook.olog.ui.websocket.MessageType;
import org.phoebus.logbook.olog.ui.websocket.WebSocketMessage;
import org.phoebus.olog.es.api.Preferences;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A basic controller for any ui performing logbook queries. The
 * controller takes care of performing the query off the ui thread using
 * {@link Job}s and then invokes the setLogs method on the UI thread after
 * the query has been completed.
 *
 * @author Kunal Shroff
 */
public abstract class LogbookSearchController implements WebSocketMessageHandler {

    protected LogClient client;
    protected final SimpleBooleanProperty searchInProgress = new SimpleBooleanProperty(false);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = Logger.getLogger(LogbookSearchController.class.getName());
    protected final SimpleBooleanProperty webSocketConnected = new SimpleBooleanProperty();
    private static final int SEARCH_JOB_INTERVAL = 30; // 30 seconds
    private ScheduledFuture<?> runningTask;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Job logbookSearchJob;
    protected final ObjectProperty<ConnectivityMode> connectivityModeObjectProperty =
            new SimpleObjectProperty<>(ConnectivityMode.NOT_CONNECTED);

    protected WebSocketClientService webSocketClientService;
    private final String webSocketConnectUrl;
    private final String subscriptionEndpoint;
    protected final CountDownLatch connectivityCheckerCountDownLatch = new CountDownLatch(1);

    @SuppressWarnings("unused")
    @FXML
    private VBox errorPane;

    @SuppressWarnings("unused")
    @FXML
    private GridPane viewSearchPane;

    public LogbookSearchController() {
        String baseUrl = Preferences.olog_url;
        URI uri = URI.create(baseUrl);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String webSocketScheme = scheme.toLowerCase().startsWith("https") ? "wss" : "ws";
        this.webSocketConnectUrl = webSocketScheme + "://" + host + (port > -1 ? (":" + port) : "") + path + "/web-socket";
        this.subscriptionEndpoint = path + "/web-socket/messages";
    }

    /**
     * Determines how the client may connect to the remote service. The service info endpoint is called to establish
     * availability of the service. If available, then a single web socket connection is attempted to determine
     * if the service supports web sockets.
     * @param consumer {@link Consumer} called when the connectivity mode has been determined.
     */
    protected void determineConnectivity(Consumer<ConnectivityMode> consumer){

        // Try to determine the connection mode: is the remote service available at all?
        // If so, does it accept web socket connections?
        JobManager.schedule("Connection mode probe", monitor -> {
            ConnectivityMode connectivityMode = ConnectivityMode.NOT_CONNECTED;
            String serviceInfo = client.serviceInfo();
            if (serviceInfo != null && !serviceInfo.isEmpty()) { // service online, check web socket availability
                if (WebSocketClientService.checkAvailability(this.webSocketConnectUrl)) {
                    connectivityMode = ConnectivityMode.WEB_SOCKETS_SUPPORTED;
                } else {
                    connectivityMode = ConnectivityMode.HTTP_ONLY;
                }
            }
            consumer.accept(connectivityMode);
            if (connectivityMode.equals(ConnectivityMode.NOT_CONNECTED)) {
                Platform.runLater(() -> {
                    errorPane.visibleProperty().set(true);
                    viewSearchPane.visibleProperty().set(false);
                });
            }
        });
    }

    public void setClient(LogClient client) {
        this.client = client;
    }

    public LogClient getLogClient() {
        return client;
    }

    /**
     * Starts a single search job. This should be used to search once.
     *
     * @param searchParams  The search parameters
     * @param resultHandler Handler taking care of the search result.
     * @param errorHandler  Client side error handler that should notify user.
     */
    public void search(Map<String, String> searchParams, final Consumer<SearchResult> resultHandler, final BiConsumer<String, Exception> errorHandler) {
        cancelPeriodSearch();
        LogbookSearchJob.submit(this.client,
                searchParams,
                resultHandler,
                errorHandler);
    }

    /**
     * Starts a search job every {@link #SEARCH_JOB_INTERVAL} seconds. If a search fails (e.g. service off-line or invalid search parameters),
     * the period search is cancelled. User will need to implicitly start it again through a "manual" search in the UI.
     *
     * @param searchParams  The search parameters
     * @param resultHandler Handler taking care of the search result.
     */
    public void periodicSearch(Map<String, String> searchParams, final Consumer<SearchResult> resultHandler) {
        cancelPeriodSearch();
        runningTask = executor.scheduleAtFixedRate(() -> logbookSearchJob = LogbookSearchJob.submit(this.client,
                searchParams,
                resultHandler,
                (url, ex) -> {
                    searchInProgress.set(false);
                    cancelPeriodSearch();
                }), SEARCH_JOB_INTERVAL, SEARCH_JOB_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Stops periodic search and ongoing search jobs, if any.
     */
    private void cancelPeriodSearch() {
        if (runningTask != null) {
            runningTask.cancel(true);
        }

        if (logbookSearchJob != null) {
            logbookSearchJob.cancel();
        }
    }

    @Deprecated
    public abstract void setLogs(List<LogEntry> logs);

    /**
     * Utility method to cancel any ongoing periodic search jobs or close web socket.
     */
    public void shutdown() {
        try {
            connectivityCheckerCountDownLatch.await(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Logger.getLogger(LogbookSearchController.class.getName()).log(Level.WARNING, "Got InterruptedException waiting for connectivity mode result");
        }
        if (connectivityModeObjectProperty.get().equals(ConnectivityMode.WEB_SOCKETS_SUPPORTED) && webSocketClientService != null) {
            Logger.getLogger(LogbookSearchController.class.getName()).log(Level.INFO, "Shutting down web socket");
            webSocketClientService.removeWebSocketMessageHandler(this);
            webSocketClientService.shutdown();
        }
        else if(connectivityModeObjectProperty.get().equals(ConnectivityMode.HTTP_ONLY)){
            cancelPeriodSearch();
        }
    }

    protected abstract void search();

    protected void connectWebSocket() {
        webSocketClientService = new WebSocketClientService(() -> {
            logger.log(Level.INFO, "Connected to web socket on " + webSocketConnectUrl);
            webSocketConnected.setValue(true);
            viewSearchPane.visibleProperty().set(true);
            errorPane.visibleProperty().set(false);
            search();
        }, () -> {
            logger.log(Level.INFO, "Disconnected from web socket on " + webSocketConnectUrl);
            webSocketConnected.set(false);
            viewSearchPane.visibleProperty().set(false);
            errorPane.visibleProperty().set(true);
        }, webSocketConnectUrl, subscriptionEndpoint, null);
        webSocketClientService.addWebSocketMessageHandler(this);
        webSocketClientService.connect();
    }

    @Override
    public void handleWebSocketMessage(String message) {
        try {
            WebSocketMessage webSocketMessage = objectMapper.readValue(message, WebSocketMessage.class);
            if (webSocketMessage.messageType().equals(MessageType.NEW_LOG_ENTRY)) {
                // Add a random sleep 0 - 5 seconds to avoid an avalanche of search requests on the service.
                long randomSleepTime = Math.round(5000 * Math.random());
                try {
                    Thread.sleep(randomSleepTime);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Got exception when sleeping before search request", e);
                }
                search();
            }
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Unable to deserialize message \"" + message + "\"");
        }
    }

    /**
     * Enum to indicate if and how the client may connect to remote Olog service.
     */
    protected enum ConnectivityMode {
        NOT_CONNECTED,
        HTTP_ONLY,
        WEB_SOCKETS_SUPPORTED
    }
}
