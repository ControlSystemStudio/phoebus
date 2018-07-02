package org.phoebus.applications.filebrowser;

import org.phoebus.framework.spi.ToolbarEntry;
import org.phoebus.framework.workbench.ApplicationService;

public class FileBrowserToolbarEntry implements ToolbarEntry {

    @Override
    public String getName() {
        return FileBrowserApp.DisplayName;
    }

    @Override
    public void call() throws Exception {
        ApplicationService.createInstance(FileBrowserApp.Name);
    }
}
