package org.phoebus.applications.queueserver;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.applications.queueserver.client.RunEngineHttpClient;
import org.phoebus.applications.queueserver.view.ViewFactory;
import javafx.scene.Parent;

import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

@SuppressWarnings("nls")
public final class QueueServerApp implements AppResourceDescriptor {

    public static final Logger logger = Logger.getLogger(QueueServerApp.class.getPackageName());

    public  static final String NAME         = "queue-server";
    private static final String DISPLAY_NAME = "Queue Server";

    @Override public String getName()        { return NAME; }
    @Override public String getDisplayName() { return DISPLAY_NAME; }

    @Override public URL getIconURL() {
        return getClass().getResource("/icons/bluesky.png");   // add one or reuse probe.png
    }

    @Override public AppInstance create() {

        // Resolve server URL with default fallback
        String serverUrl = Preferences.queue_server_url;
        // Check if the preference wasn't expanded (still has $(VAR) syntax) or is empty
        if (serverUrl == null || serverUrl.trim().isEmpty() || serverUrl.startsWith("$(")) {
            serverUrl = "http://localhost:60610";
            logger.log(Level.INFO, "Using default Queue Server URL: " + serverUrl);
        }

        // Resolve API key with priority:
        // 1. Direct api_key preference (or QSERVER_HTTP_SERVER_API_KEY env var)
        // 2. Read from api_key_file path (or QSERVER_HTTP_SERVER_API_KEYFILE env var)
        String apiKey = resolveApiKey();

        RunEngineHttpClient.initialize(serverUrl, apiKey);

        Parent root = ViewFactory.APPLICATION.get();

        QueueServerInstance inst = new QueueServerInstance(this, root);

        DockItem tab = new DockItem(inst, root);
        DockPane.getActiveDockPane().addTab(tab);

        return inst;
    }

    /**
     * Resolve the API key using the same priority as Python bluesky-widgets:
     * 1. Check QSERVER_HTTP_SERVER_API_KEY environment variable (via api_key preference)
     * 2. If not set, check QSERVER_HTTP_SERVER_API_KEYFILE environment variable (via api_key_file preference)
     * 3. If keyfile path is set, read the API key from that file
     *
     * @return The resolved API key, or null if not configured
     */
    private static String resolveApiKey() {
        // First priority: direct API key
        String apiKey = Preferences.api_key;
        // Check if the preference was expanded (not still $(VAR) syntax) and not empty
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("$(")) {
            logger.log(Level.FINE, "Using API key from QSERVER_HTTP_SERVER_API_KEY");
            return apiKey.trim();
        }

        // Second priority: read from keyfile
        String keyFilePath = Preferences.api_key_file;
        if (keyFilePath != null && !keyFilePath.trim().isEmpty() && !keyFilePath.startsWith("$(")) {
            try {
                Path path = Paths.get(keyFilePath.trim());
                if (Files.exists(path)) {
                    apiKey = Files.readString(path).trim();
                    logger.log(Level.FINE, "Using API key from file: " + keyFilePath);
                    return apiKey;
                } else {
                    logger.log(Level.WARNING, "API key file not found: " + keyFilePath);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to read API key from file: " + keyFilePath, e);
            }
        }

        logger.log(Level.WARNING, "No API key configured. Set QSERVER_HTTP_SERVER_API_KEY environment variable " +
                "or QSERVER_HTTP_SERVER_API_KEYFILE to point to a file containing the API key.");
        return null;
    }

    @Override public AppInstance create(java.net.URI resource) {
        return ApplicationService.createInstance(NAME);
    }
}