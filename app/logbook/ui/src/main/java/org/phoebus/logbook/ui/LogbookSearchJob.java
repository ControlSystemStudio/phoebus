package org.phoebus.logbook.ui;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobRunnableWithCancel;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;

/**
 * Background job for searching log entries
 * 
 * @author Kunal Shroff, Kay Kasemir
 */
public class LogbookSearchJob extends JobRunnableWithCancel {
    private final LogClient client;
    private final Map<String, String> searchMap;
    private final Consumer<List<LogEntry>> logEntryHandler;
    private final BiConsumer<String, Exception> errorHandler;

    /**
     * Submit a logbook search query
     * @param client the logbook client
     * @param searchMap the search parameters
     * @param logEntryHandler consumer for the list of {@link LogEntry}s from the search
     * @param errorHandler error handler
     * @return a logbook search job
     */
    public static Job submit(LogClient client, final Map<String, String> searchMap,
            final Consumer<List<LogEntry>> logEntryHandler, final BiConsumer<String, Exception> errorHandler) {
        return JobManager.schedule("searching logbook for : " + searchMap,
                new LogbookSearchJob(client, searchMap, logEntryHandler, errorHandler));
    }

    private LogbookSearchJob(LogClient client, Map<String, String> searchMap, Consumer<List<LogEntry>> logEntryHandler,
            BiConsumer<String, Exception> errorHandler) {
        super();
        this.client = client;
        this.searchMap = searchMap;
        this.logEntryHandler = logEntryHandler;
        this.errorHandler = errorHandler;
    }

    @Override
    public String getName() {
        return "searching for log entries : " + searchMap;
    }

    @Override
    public Runnable getRunnable() {
        return () -> {
            List<LogEntry> logEntries = client.findLogs(searchMap);
            logEntryHandler.accept(logEntries);
        };
    }
}
