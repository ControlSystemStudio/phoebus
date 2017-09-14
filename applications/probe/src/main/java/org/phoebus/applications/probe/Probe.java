package org.phoebus.applications.probe;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

public class Probe implements AppDescriptor {

    public static final String NAME = "Probe";

    public String getName() {
        return NAME;
    }

    public void start() {
    }

    public void stop() {
    }

    @Override
    public AppInstance create() {
        //TODO
        return null;
    }

}
