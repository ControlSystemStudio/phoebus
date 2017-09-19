package org.phoebus.applications.greetings;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

@SuppressWarnings("nls")
public class GreetingsApp implements AppDescriptor {

    public static final String Name = "Greetings";

    @Override
    public String getName() {
        return Name;
    }

    @Override
    public AppInstance create() {
        return new Greet(this);
    }
}
