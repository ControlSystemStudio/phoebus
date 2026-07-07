package org.phoebus.logbook.olog.ui.write;

import org.phoebus.logbook.LogEntry;
import org.phoebus.olog.es.api.model.OlogLog;
import java.util.List;

public class LogEntryUtils {

    public static LogEntry createLogEntryFromList(String baseURL, List<LogEntry> logEntries){
        OlogLog log = new OlogLog();
        StringBuilder stringBuilder = new StringBuilder();
        logEntries.forEach(l -> {
            stringBuilder.append("\n");
            stringBuilder.append("- [").append(l.getTitle()).append("](").append(baseURL).append("/").append(l.getId()).append(")");
        });

        log.setSource(stringBuilder.toString());
        return log;
    }
}
