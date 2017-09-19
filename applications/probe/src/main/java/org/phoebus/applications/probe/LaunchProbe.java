package org.phoebus.applications.probe;

import org.phoebus.framework.spi.ToolbarEntry;
import org.phoebus.framework.workbench.ApplicationService;

// @ProviderFor(ToolbarEntry.class)
public class LaunchProbe implements ToolbarEntry {

    @Override
    public String getName() {
        return Probe.NAME;
    }

    @Override
    public void call() throws Exception {
        ApplicationService.findApplication(Probe.NAME).create();
    }

}
