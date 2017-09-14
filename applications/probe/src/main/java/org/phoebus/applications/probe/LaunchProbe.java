package org.phoebus.applications.probe;

import org.phoebus.framework.spi.ToolbarEntry;

// @ProviderFor(ToolbarEntry.class)
public class LaunchProbe implements ToolbarEntry {

    @Override
    public String getName() {
        return Probe.NAME;
    }

    @Override
    public void call() throws Exception {
        new Probe().create();
    }

}
