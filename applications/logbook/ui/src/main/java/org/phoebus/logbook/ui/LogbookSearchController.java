package org.phoebus.logbook.ui;

import java.util.List;

import org.phoebus.framework.jobs.Job;
import org.phoebus.logbook.utility.LogbookSearchJob;
import org.phoebus.logging.LogClient;
import org.phoebus.logging.LogEntry;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import javafx.application.Platform;

/**
 * A basic controller for any ui performing logbook queries. The
 * controller takes care of performing the query off the ui thread using
 * {@link Job}s and then invokes the setLogs method on the UI thread after
 * the query has been completed.
 * 
 * @author Kunal Shroff
 *
 */
public abstract class LogbookSearchController {

    private Job logbookSearchJob;
    private LogClient client;

    public void setClient(LogClient client) {
        this.client = client;
    }

    public void search(String searchString) {
        if (logbookSearchJob != null) {
            logbookSearchJob.cancel();
        }
        logbookSearchJob = LogbookSearchJob.submit(this.client,
                searchString,
                logs -> Platform.runLater(() -> setLogs(logs)),
                (url, ex) -> ExceptionDetailsErrorDialog.openError("Logbook Search Error", ex.getMessage(), ex));
    }

    public abstract void setLogs(List<LogEntry> logs);
}
