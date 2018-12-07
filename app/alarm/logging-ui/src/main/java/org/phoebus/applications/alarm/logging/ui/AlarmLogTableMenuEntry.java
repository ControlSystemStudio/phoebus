package org.phoebus.applications.alarm.logging.ui;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

public class AlarmLogTableMenuEntry implements MenuEntry {

    @Override
    public Void call() throws Exception {
        ApplicationService.createInstance(AlarmLogTableApp.NAME);
        return null;
    }

    @Override
    public String getName() {
        return AlarmLogTableApp.DISPLAYNAME;
    }

    @Override
    public String getMenuPath() {
        return "Alarm";
    }
    
    @Override
    public Image getIcon() {
        return AlarmLogTableApp.icon;
    }

}
