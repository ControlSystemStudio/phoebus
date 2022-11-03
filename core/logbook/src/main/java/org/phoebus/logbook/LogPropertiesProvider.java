package org.phoebus.logbook;

import java.util.Collections;
import java.util.List;

/**
 * An interface to subscribe property providers. A custom logbook module can use this
 * to - for instance - add a list of {@link Property} objects calculated dynamically when a new
 * log entry is created.
 */
public interface LogPropertiesProvider {

    /**
     * Clients should consider that implementations may take some time to complete, e.g. query a remote
     * endpoint for data.
     * @param logEntry The {@link LogEntry} object as created by the client. Implementations can use this to apply filtering
     *            based on the fields of the log entry.
     * @return A {@link Property} added to a new log entry. An implementation should return <code>null</code>
     * if it applies a filter that rules out the log entry.
     */
    @SuppressWarnings("unused")
    default List<Property> getProperties(LogEntry logEntry){
        return Collections.emptyList();
    }

    /**
     * Clients should consider that implementations may take some time to complete, e.g. query a remote
     * endpoint for data.
     * @return A {@link Property} added to a new log entry. An implementation should return <code>null</code>
     * if it applies a filter that rules out the log entry.
     */
    default List<Property> getProperties(){
        return Collections.emptyList();
    }
}
