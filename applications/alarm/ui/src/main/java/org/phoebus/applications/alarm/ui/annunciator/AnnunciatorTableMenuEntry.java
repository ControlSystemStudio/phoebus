package org.phoebus.applications.alarm.ui.annunciator;

import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.ApplicationService;

import javafx.scene.image.Image;

public class AnnunciatorTableMenuEntry implements MenuEntry
{

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(AnnunciatorTableApplication.NAME);
        return null;
    }

    @Override
    public String getName()
    {
        return AnnunciatorTableApplication.DISPLAY_NAME;
    }

    @Override
    public Image getIcon()
    {
        //return ImageCache.getImage(AlarmUI.class, "/icons/annunciator.png");
        return null;
    }

    @Override
    public String getMenuPath()
    {
        return "Alarm";
    }

}
