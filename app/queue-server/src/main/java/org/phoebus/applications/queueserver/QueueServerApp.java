package org.phoebus.applications.queueserver;

import java.net.URL;
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

    public  static final Logger LOGGER = Logger.getLogger(QueueServerApp.class.getPackageName());

    public  static final String NAME         = "queue-server";
    private static final String DISPLAY_NAME = "Queue Server";

    @Override public String getName()        { return NAME; }
    @Override public String getDisplayName() { return DISPLAY_NAME; }

    @Override public URL getIconURL() {
        return getClass().getResource("/icons/bluesky.png");   // add one or reuse probe.png
    }

    @Override public AppInstance create() {

        RunEngineHttpClient.initialize(Preferences.queue_server_url, Preferences.api_key);

        Parent root = ViewFactory.APPLICATION.get();

        QueueServerInstance inst = new QueueServerInstance(this, root);

        DockItem tab = new DockItem(inst, root);
        DockPane.getActiveDockPane().addTab(tab);

        return inst;
    }

    @Override public AppInstance create(java.net.URI resource) {
        return ApplicationService.createInstance(NAME);
    }
}