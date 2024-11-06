package org.phoebus.logbook.olog.ui.spi;

import javafx.scene.Node;
import org.phoebus.logbook.LogEntry;

import java.util.List;

public interface Decoration {
    public Node getDecorationInputNode();

    public void setLogEntries(List<LogEntry> logEntries);

    public void setRefreshLogEntryTableView(Runnable refreshLogEntryTableView);

    public Node createDecorationForLogEntryCell(LogEntry logEntry);
}
