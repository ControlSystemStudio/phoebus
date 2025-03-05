/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore;

import javafx.scene.image.Image;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ToolbarEntry;

public class SaveAndRestoreToolbarEntry implements ToolbarEntry {

    @Override
    public String getName() {
        return SaveAndRestoreApplication.DISPLAY_NAME;
    }

    @Override
    public Image getIcon() {
        return ImageCache.getImage(getClass(), "/icons/save-and-restore.png");
    }

    @Override
    public Void call() throws Exception {
        ApplicationService.createInstance(SaveAndRestoreApplication.NAME);
        return null;
    }

    /**
     * DO NOT CHANGE RETURN VALUE!
     * @return The unique id of this {@link ToolbarEntry}.
     */
    @Override
    public String getId(){
        return "Save And Restore";
    }
}
