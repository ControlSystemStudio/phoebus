package org.phoebus.logbook.olog.ui;

import javafx.beans.property.SimpleBooleanProperty;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobRunnableWithCancel;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.SearchResult;
import org.phoebus.olog.es.api.OlogClient;
import org.phoebus.olog.es.api.model.OlogSearchResult;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background job for searching log entries
 * 
 * @author Kunal Shroff, Kay Kasemir
 */
public class LogbookSearchJob extends JobRunnableWithCancel {
    private final LogClient client;
    private final Map<String, String> searchMap;
    private final Consumer<SearchResult> logEntryHandler;
    private final BiConsumer<String, Exception> errorHandler;

    /**
     * Submit a logbook search query
     * @param client the logbook client
     * @param searchMap the search parameters
     * @param logEntryHandler consumer for the {@link SearchResult} from the search
     * @param errorHandler error handler
     * @return a logbook search job
     */
    public static Job submit(LogClient client, final Map<String, String> searchMap,
                             final Consumer<SearchResult> logEntryHandler, final BiConsumer<String, Exception> errorHandler) {
        return JobManager.schedule("searching logbook for : " + searchMap,
                new LogbookSearchJob(client, searchMap, logEntryHandler, errorHandler));
    }

    private LogbookSearchJob(LogClient client, Map<String, String> searchMap, Consumer<SearchResult> logEntryHandler,
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
            try {
                SearchResult searchResult = client.search(searchMap);
                logEntryHandler.accept(searchResult);
            } catch (Exception exception) {
                Logger.getLogger(LogbookSearchJob.class.getName())
                        .log(Level.SEVERE, "Failed to obtain logs", exception);
                errorHandler.accept(null, exception);
            }
        };
    }
}
