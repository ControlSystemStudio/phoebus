package org.phoebus.applications.queueserver.monitor;

import org.phoebus.applications.queueserver.Messages;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;
import javafx.scene.image.Image;

public final class QueueMonitorMenuEntry implements MenuEntry {

    @Override public String getName()      { return Messages.QueueMonitor; }
    @Override public Image  getIcon()      { return ImageCache.getImage(
            QueueMonitorApp.class, "/icons/bluesky.png"); }
    @Override public String getMenuPath()  { return Messages.QueueMonitorMenuPath; }

    @Override public Void call() throws Exception {
        ApplicationService.createInstance(QueueMonitorApp.NAME);
        return null;
    }
}
