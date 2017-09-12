package org.phoebus.applications.probe;

import org.phoebus.framework.spi.AppDescriptor;

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
	public void open() {
		
	}

}
