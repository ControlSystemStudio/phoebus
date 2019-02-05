package org.phoebus.app.diag;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.MenuEntry;

public class DiagMenuEntry implements MenuEntry {

    @Override
    public String getName() {
        return DiagApp.NAME;
    }

    @Override
    public Void call() throws Exception {
        ApplicationService.createInstance(DiagApp.NAME);
        return null;
    }

    @Override
    public String getMenuPath() {
        return "Debug";
    }
}
