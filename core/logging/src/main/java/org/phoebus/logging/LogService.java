package org.phoebus.logging;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A service for creating log entries into the registered log clients.
 * 
 * TODO the service might be less confusing if it simply returned the registered
 * {@link LogFactory}'s and then has the calling code submit logging request to
 * the clients.
 * 
 * @author Kunal Shroff
 *
 */
public class LogService {

    static final java.lang.String SERVICE_NAME = "LoggingService";

    private static LogService logService;
    private ServiceLoader<LogFactory> loader;
    private Map<String, LogFactory> logFactories;

    private static ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 
     */
    private LogService() {
        // Load available adapter factories
        logFactories = new HashMap<String, LogFactory>();
        loader = ServiceLoader.load(LogFactory.class);
        loader.stream().forEach(p -> {
            LogFactory logFactory = p.get();
            logFactories.put(logFactory.getId(), logFactory);
        });
    }

    public static LogService getInstance() {
        if (logService == null) {
            logService = new LogService();
        }
        return logService;
    }

    public Map<String, LogFactory> getLogFactories() {
        return Collections.unmodifiableMap(logFactories);
    }

    /**
     * Create a log entry in all register LogFactory
     * 
     * @param adaptedSelections
     */
    public void createLogEntry(LogEntry logEntry) {
        executor.submit(() -> {
            logFactories.values().stream().forEach(logFactory -> {
                logFactory.getLogClient().set(logEntry);
            });
        });
    }

    /**
     * Create a log entry in all register LogFactory TODO change to Log type
     * 
     * @param adaptedSelections
     */
    public void createLogEntry(List<LogEntry> logEntries) {
        executor.submit(() -> {
            logFactories.values().stream().forEach(logFactory -> {
                logEntries.forEach(logEntry -> {
                    logFactory.getLogClient().set(logEntry);
                });
            });
        });
    }

    /**
     * Create a log entry in the specified LogFactory TODO change to Log type
     * 
     * @param id
     * @param log
     */
    public void createLogEntry(String id, LogEntry logEntry) {
        executor.submit(() -> {
            logFactories.get(id).getLogClient().set(logEntry);
        });
    }
}
