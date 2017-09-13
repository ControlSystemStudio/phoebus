package org.phoebus.applications.greetings;

import org.phoebus.framework.spi.AppDescriptor;

public class GreetingsApp implements AppDescriptor {

    public static final String Name = "Greetings";

    public String getName() {
        return Name;
    }

    public void start() {
    }

    public void stop() {
    }

	@Override
	public void open() {
		
	}

}
