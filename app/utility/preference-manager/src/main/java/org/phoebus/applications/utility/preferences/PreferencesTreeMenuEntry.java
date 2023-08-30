package org.phoebus.applications.utility.preferences;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.MenuEntry;

/**
 * Menu Action for launching the preferences tree viewer
 */
public class PreferencesTreeMenuEntry implements MenuEntry {
    @Override
    public String getName() {
        return PreferencesApp.DISPLAY_NAME;
    }

    @Override
    public String getMenuPath() {
        return "Utility";
    }

    @Override
    public Void call() throws Exception {
        ApplicationService.createInstance(PreferencesApp.NAME);
        return null;
    }
}
