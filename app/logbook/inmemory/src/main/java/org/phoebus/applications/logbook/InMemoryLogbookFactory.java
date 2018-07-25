package org.phoebus.applications.logbook;

import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogFactory;

public class InMemoryLogbookFactory implements LogFactory {

    private static String ID = "inmemory";
    private InMemoryLogClient inMemoryLogClient = new InMemoryLogClient();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LogClient getLogClient() {
        return inMemoryLogClient;
    }

    @Override
    public LogClient getLogClient(Object authToken) {
        return inMemoryLogClient;
    }

}
