package org.phoebus.applications.eslog;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

@SuppressWarnings("nls")
public class EsLogMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return EsLogApp.DISPLAY_NAME;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(Activator.class, "/icons/eslog.png");
    }

    @Override
    public String getMenuPath()
    {
        return "Utility";
    }

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(EsLogApp.NAME);
        return null;
    }
}
