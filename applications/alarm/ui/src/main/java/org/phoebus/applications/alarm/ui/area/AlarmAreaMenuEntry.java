package org.phoebus.applications.alarm.ui.area;

import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.ApplicationService;

import javafx.scene.image.Image;

public class AlarmAreaMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return AlarmAreaApplication.DISPLAY_NAME;
    }

    @Override
    public Image getIcon()
    {
       // return ImageCache.getImage(AlarmUI.class, "/icons/alarmarea.png");
    	return null;
    }

    @Override
    public String getMenuPath()
    {
        return "Alarm";
    }

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(AlarmAreaApplication.NAME);
        return null;
    }
}
