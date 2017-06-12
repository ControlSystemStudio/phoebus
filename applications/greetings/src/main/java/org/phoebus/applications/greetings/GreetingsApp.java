package org.phoebus.applications.greetings;

import org.phoebus.framework.annotation.ProviderFor;
import org.phoebus.framework.spi.Application;

@ProviderFor(Application.class)
public class GreetingsApp implements Application {

    public static final String Name = "Greetings";

    public String getName() {
        return Name;
    }

    public void start() {
    }

    public void stop() {
    }

}
