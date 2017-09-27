package org.phoebus.applications.probe;

import org.phoebus.framework.spi.ToolbarEntry;
import org.phoebus.framework.workbench.ApplicationService;

/**
 * Launching the probe from the main toolbar
 * @author Kunal Shroff
 *
 */
public class ProbeToolbarEntry implements ToolbarEntry {

    @Override
    public String getName() {
        return Probe.DISPLAYNAME;
    }

    @Override
    public void call() throws Exception {
        ApplicationService.findApplication(Probe.NAME).create();
    }

}
