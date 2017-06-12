package org.phoebus.applications.probe;

import java.util.concurrent.Callable;

import org.phoebus.framework.annotation.ProviderFor;
import org.phoebus.framework.spi.ToolbarEntry;

@ProviderFor(ToolbarEntry.class)
public class LaunchProbe implements ToolbarEntry {

    private static final String NAME = "Probe";

    public String getName() {
        return NAME;
    }

    public <T> Callable<T> getActions() {
        return null;
    }

}
