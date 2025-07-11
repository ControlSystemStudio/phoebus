package org.phoebus.applications.queueserver;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;
import javafx.scene.image.Image;

public final class QueueServerMenuEntry implements MenuEntry {

    @Override public String getName()      { return Messages.QueueServer; }
    @Override public Image  getIcon()      { return ImageCache.getImage(
            QueueServerApp.class,
            "/icons/bluesky.png"); }   // same icon as descriptor
    @Override public String getMenuPath()  { return Messages.QueueServerMenuPath; }

    @Override public Void call() throws Exception {
        ApplicationService.createInstance(QueueServerApp.NAME);
        return null;
    }
}
