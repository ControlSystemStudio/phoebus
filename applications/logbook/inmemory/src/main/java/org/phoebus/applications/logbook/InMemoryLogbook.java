package org.phoebus.applications.logbook;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.phoebus.framework.annotation.ProviderFor;
import org.phoebus.logging.LogEntry;
import org.phoebus.logging.LogFactory;

@ProviderFor(LogFactory.class)
public class InMemoryLogbook implements LogFactory {

    private static final String ID = "org.phoebus.inmemory.log";
    private static final Map<Instant, String> inMemoryLog = new HashMap<Instant, String>();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void createLogEntry(LogEntry log) {
        inMemoryLog.put(log.getTime(), log.toString());
    }

    @Override
    public List<String> findLogEntries(Map<String, String> searchParameters) {
        // TODO
        return null;
    }

}
