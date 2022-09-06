package org.phoebus.logbook.olog.ui;

import javafx.beans.property.SimpleBooleanProperty;
import org.phoebus.framework.jobs.Job;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.SearchResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A basic controller for any ui performing logbook queries. The
 * controller takes care of performing the query off the ui thread using
 * {@link Job}s and then invokes the setLogs method on the UI thread after
 * the query has been completed.
 *
 * @author Kunal Shroff
 */
public abstract class LogbookSearchController {

    private Job logbookSearchJob;
    protected LogClient client;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> runningTask;
    protected final SimpleBooleanProperty searchInProgress = new SimpleBooleanProperty(false);
    private static final int SEARCH_JOB_INTERVAL = 30; // seconds

    public void setClient(LogClient client) {
        this.client = client;
    }

    public LogClient getLogClient(){
        return client;
    }

    /**
     * Starts a single search job. This should be used to search once.
     * @param searchParams The search parameters
     * @param resultHandler Handler taking care of the search result.
     * @param errorHandler Client side error handler that should notify user.
     */
    public void search(Map<String, String> searchParams, final Consumer<SearchResult> resultHandler, final BiConsumer<String, Exception> errorHandler) {
        cancelPeriodSearch();
        logbookSearchJob = LogbookSearchJob.submit(this.client,
                searchParams,
                resultHandler,
                errorHandler);
    }

    /**
     * Starts a search job every {@link #SEARCH_JOB_INTERVAL} seconds. If a search fails (e.g. service off-line or invalid search parameters),
     * the period search is cancelled. User will need to implicitly start it again through a "manual" search in the UI.
     * @param searchParams The search parameters
     * @param resultHandler Handler taking care of the search result.
     */
    public void periodicSearch(Map<String, String> searchParams, final Consumer<SearchResult> resultHandler) {
        cancelPeriodSearch();
        runningTask = executor.scheduleAtFixedRate(() -> {
            logbookSearchJob = LogbookSearchJob.submit(this.client,
                    searchParams,
                    resultHandler,
                    (url, ex) -> {
                        searchInProgress.set(false);
                        cancelPeriodSearch();
                    });
        }, 0, SEARCH_JOB_INTERVAL, TimeUnit.SECONDS);
    }

    @Deprecated
    public abstract void setLogs(List<LogEntry> logs);

    /**
     * Stops periodic search and ongoing search jobs, if any.
     */
    private void cancelPeriodSearch() {
        if (runningTask != null) {
            runningTask.cancel(true);
        }

        if (logbookSearchJob != null) {
            logbookSearchJob.cancel();
        }
    }

    /**
     * Utility method to cancel any ongoing periodic search jobs.
     */
    public void shutdown() {
        cancelPeriodSearch();
    }
}
