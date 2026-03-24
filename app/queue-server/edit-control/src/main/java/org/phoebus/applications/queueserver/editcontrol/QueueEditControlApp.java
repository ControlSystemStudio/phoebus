package org.phoebus.applications.queueserver.editcontrol;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.applications.queueserver.Preferences;
import org.phoebus.applications.queueserver.client.RunEngineHttpClient;
import org.phoebus.applications.queueserver.util.AppLifecycle;
import org.phoebus.applications.queueserver.util.PythonParameterConverter;
import org.phoebus.applications.queueserver.view.ViewFactory;
import javafx.scene.Parent;

import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

@SuppressWarnings("nls")
public final class QueueEditControlApp implements AppResourceDescriptor {

    public static final Logger logger = Logger.getLogger(QueueEditControlApp.class.getPackageName());

    public  static final String NAME         = "queue_edit_control";
    private static final String DISPLAY_NAME = "Edit & Control Queue";

    static {
        PythonParameterConverter.initializeInBackground();
    }

    @Override public String getName()        { return NAME; }
    @Override public String getDisplayName() { return DISPLAY_NAME; }

    @Override public URL getIconURL() {
        return getClass().getResource("/icons/bluesky.png");
    }

    @Override public AppInstance create() {
        initializeHttpClient();
        AppLifecycle.registerApp();

        Parent root = ViewFactory.EDIT_AND_CONTROL_QUEUE.get();
        QueueEditControlInstance inst = new QueueEditControlInstance(this, root);

        DockItem tab = new DockItem(inst, root);
        tab.addClosedNotification(AppLifecycle::unregisterApp);
        DockPane.getActiveDockPane().addTab(tab);

        return inst;
    }

    @Override public AppInstance create(java.net.URI resource) {
        return ApplicationService.createInstance(NAME);
    }

    private static void initializeHttpClient() {
        String serverUrl = Preferences.queue_server_url;
        if (serverUrl == null || serverUrl.trim().isEmpty() || serverUrl.startsWith("$(")) {
            serverUrl = "http://localhost:60610";
            logger.log(Level.INFO, "Using default Queue Server URL: " + serverUrl);
        }
        String apiKey = resolveApiKey();
        RunEngineHttpClient.initialize(serverUrl, apiKey);
    }

    private static String resolveApiKey() {
        String apiKey = Preferences.api_key;
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("$(")) {
            return apiKey.trim();
        }
        String keyFilePath = Preferences.api_key_file;
        if (keyFilePath != null && !keyFilePath.trim().isEmpty() && !keyFilePath.startsWith("$(")) {
            try {
                Path path = Paths.get(keyFilePath.trim());
                if (Files.exists(path)) {
                    return Files.readString(path).trim();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to read API key from file: " + keyFilePath, e);
            }
        }
        return null;
    }
}
