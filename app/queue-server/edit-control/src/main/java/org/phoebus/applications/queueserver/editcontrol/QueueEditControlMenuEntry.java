package org.phoebus.applications.queueserver.editcontrol;

import org.phoebus.applications.queueserver.Messages;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;
import javafx.scene.image.Image;

public final class QueueEditControlMenuEntry implements MenuEntry {

    @Override public String getName()      { return Messages.EditControlQueue; }
    @Override public Image  getIcon()      { return ImageCache.getImage(
            QueueEditControlApp.class, "/icons/bluesky.png"); }
    @Override public String getMenuPath()  { return Messages.EditControlQueueMenuPath; }

    @Override public Void call() throws Exception {
        ApplicationService.createInstance(QueueEditControlApp.NAME);
        return null;
    }
}
