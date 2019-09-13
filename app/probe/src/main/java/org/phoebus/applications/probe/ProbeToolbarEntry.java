package org.phoebus.applications.probe;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.ToolbarEntry;

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
    public Void call() throws Exception {
        ApplicationService.createInstance(Probe.NAME);
        return null;
    }
}
