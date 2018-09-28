package org.phoebus.app.viewer3d;

import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;

public class Viewer3dMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return Viewer3dApp.DisplayName;
    }

    @Override
    public String getMenuPath()
    {
        return "Display";
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(ImageCache.class, "/icons/viewer3d.png");
    }
    
    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(Viewer3dApp.Name);
        return null;
    }
}
