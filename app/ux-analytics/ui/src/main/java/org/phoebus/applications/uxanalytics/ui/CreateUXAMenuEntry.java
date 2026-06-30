package org.phoebus.applications.uxanalytics.ui;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.MenuEntry;

public class CreateUXAMenuEntry implements MenuEntry {

    @Override
    public String getName() { return UXAnalyticsMain.DISPLAY_NAME;}

    @Override
    public Void call() throws Exception {
        ApplicationService.createInstance(UXAnalyticsMain.NAME);
        return null;
    }

    @Override
    public String getMenuPath() {
        return "Utility";
    }

}