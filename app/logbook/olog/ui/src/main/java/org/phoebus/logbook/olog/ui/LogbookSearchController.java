package org.phoebus.logbook.olog.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.phoebus.core.websocket.WebSocketMessageHandler;
import org.phoebus.core.websocket.springframework.WebSocketClientService;
import org.phoebus.framework.jobs.Job;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.SearchResult;
import org.phoebus.logbook.olog.ui.websocket.MessageType;
import org.phoebus.logbook.olog.ui.websocket.WebSocketMessage;
import org.phoebus.olog.es.api.Preferences;

import java.net.URI;
import java.util.List;
import java.util.Map;
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
    protected final SimpleBooleanProperty serviceConnected = new SimpleBooleanProperty();

    protected WebSocketClientService webSocketClientService;

    @SuppressWarnings("unused")
    @FXML
    private VBox errorPane;

    @SuppressWarnings("unused")
    @FXML
    private GridPane ViewSearchPane;

    @FXML
    public void initialize() {
        errorPane.visibleProperty().bind(serviceConnected.not());
        ViewSearchPane.visibleProperty().bind(serviceConnected);
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
        LogbookSearchJob.submit(this.client,
                searchParams,
                resultHandler,
                errorHandler);
    }

    @Deprecated
    public abstract void setLogs(List<LogEntry> logs);

    /**
     * Utility method to cancel any ongoing periodic search jobs.
     */
    public void shutdown() {
        if (webSocketClientService != null) {
            Logger.getLogger(LogbookSearchController.class.getName()).log(Level.INFO, "Shutting down web socket");
            webSocketClientService.shutdown();
        }
    }

    protected abstract void search();

    protected void connectWebSocket() {
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
        String webSocketUrl = webSocketScheme + "://" + host + (port > -1 ? (":" + port) : "") + path;

        webSocketClientService = new WebSocketClientService(() -> {
            logger.log(Level.INFO, "Connected to web socket on " + webSocketUrl);
            serviceConnected.setValue(true);
            search();
        }, () -> {
            logger.log(Level.INFO, "Disconnected from web socket on " + webSocketUrl);
            serviceConnected.set(false);
        });
        webSocketClientService.addWebSocketMessageHandler(this);
        webSocketClientService.connect(webSocketUrl);
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
}
