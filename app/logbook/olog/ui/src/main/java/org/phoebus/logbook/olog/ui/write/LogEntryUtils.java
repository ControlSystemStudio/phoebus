package org.phoebus.logbook.olog.ui.write;

import org.phoebus.logbook.LogEntry;
import org.phoebus.olog.es.api.model.OlogLog;

import java.util.List;

public class LogEntryUtils {

    public static LogEntry createLogEntryFromList(String baseURL, List<LogEntry> logEntries){
        // number of log entries
        // they links should be based on the
        // instantiate new Log Entry implementatin Olog
        // interface: type of class that defines a behavior/functionality/data
        // you cannot instantiate an interface
        // so we instantiate an OlogLog
        // public class OlogLog implements LogEntry
        OlogLog log = new OlogLog();
        StringBuilder stringBuilder = new StringBuilder();
        logEntries.forEach(l -> {
            stringBuilder.append("\n");
            stringBuilder.append("[").append(l.getTitle()).append("](").append(baseURL).append(l.getId()).append(")\n\n");
        });

        log.setSource(stringBuilder.toString());
        return log;
    }
}
