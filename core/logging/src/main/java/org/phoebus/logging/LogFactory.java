package org.phoebus.logging;

import java.util.List;
import java.util.Map;

/**
 * An interface describing a logging service
 * 
 * @author Kunal Shroff
 *
 */
public interface LogFactory {

    /**
     * 
     * @return
     */
    public String getId();

    /**
     * Create a log entry
     * @param log
     */
    public void createLogEntry(LogEntry log);

    /**
     * Search for a list of log entries
     * @param searchParameters
     * @return
     */
    public List<String> findLogEntries(Map<String, String> searchParameters);
}
