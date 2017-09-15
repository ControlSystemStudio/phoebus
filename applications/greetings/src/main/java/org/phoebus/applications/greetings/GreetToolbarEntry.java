package org.phoebus.applications.greetings;

import java.util.Arrays;
import java.util.List;

import org.phoebus.framework.spi.ToolbarEntry;
import org.phoebus.framework.workbench.ApplicationService;

public class GreetToolbarEntry implements ToolbarEntry {

    private static final String NAME = "Greetings";

    @Override
    public String getName() {
        return GreetingsApp.Name;
    }

    public List<String> getActionNames() {
        return Arrays.asList(NAME);
    }

    @Override
    public void call() throws Exception {
        ApplicationService.findApplication(GreetingsApp.Name).create();
    }
}
