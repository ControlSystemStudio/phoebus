package org.phoebus.applications.filebrowser;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ToolbarEntry;

import javafx.scene.image.Image;

@SuppressWarnings("nls")
public class FileBrowserToolbarEntry implements ToolbarEntry {

    @Override
    public String getName() {
        return FileBrowserApp.DisplayName;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(FileBrowserApp.class, "/icons/filebrowser.png");
    }

    @Override
    public Void call() throws Exception {
        ApplicationService.createInstance(FileBrowserApp.Name);
        return null;
    }
}
