package org.phoebus.logbook.olog.ui.spi;

import javafx.scene.Node;
import org.phoebus.logbook.LogEntry;

import java.util.List;

public interface Decoration {
    void setLogEntries(List<LogEntry> logEntries);

    void setRefreshLogEntryTableView(Runnable refreshLogEntryTableView);

    Node createDecorationForLogEntryCell(LogEntry logEntry);
}
