package org.phoebus.applications.logbook;

import org.phoebus.logging.LogClient;
import org.phoebus.logging.LogFactory;

public class InMemoryLogbookFactory implements LogFactory {

    private static final String ID = "org.phoebus.inmemory.log";
    private final InMemoryLogClient inMemoryLogClient = new InMemoryLogClient();

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
