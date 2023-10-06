package org.phoebus.logbook.olog.ui;

import com.google.common.base.Strings;
import javafx.scene.image.Image;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.logbook.*;
import org.phoebus.ui.javafx.ImageCache;

import java.net.URI;
import java.util.logging.Logger;

public class LogEntryTableApp implements AppResourceDescriptor {

    public static final Logger logger = Logger.getLogger(LogEntryTableApp.class.getName());
    static final Image icon = ImageCache.getImage(LogEntryTableApp.class, "/icons/logbook-16.png");
    public static final String NAME = "logEntryTable";
    public static String DISPLAYNAME = "Log Entry Table";

    private static final String SUPPORTED_SCHEMA = "logbook";
    private LogFactory logFactory;

    private LogEntryTable logEntryTable;

    @Override
    public void start() {
        logFactory = LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory);
        String displayName = LogbookUIPreferences.log_entry_table_display_name;
        if(!Strings.isNullOrEmpty(displayName)){
            DISPLAYNAME = displayName;
        }
    }

    @Override
    public String getDisplayName() {
        return DISPLAYNAME;
    }
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AppInstance create() {
        logEntryTable = new LogEntryTable(this);
        return logEntryTable;
    }

    @Override
    public boolean canOpenResource(String resource) {
        return URI.create(resource).getScheme().equals(SUPPORTED_SCHEMA);
    }

    /**
     * Support the launching of log entry table view using resource
     * {@literal logbook://?<search_string> e.g. -resource}
     * {@literal logbook://?search=*Fault*Motor*&tag=operation}
     */
    @Override
    public AppInstance create(URI resource) {
        logEntryTable = new LogEntryTable(this);
        logEntryTable.setResource(resource);
        return logEntryTable;
    }

    public LogClient getClient() {
        return logFactory.getLogClient();
    }

    /**
     * Handler for a {@link LogEntry} change, new or updated. The log table
     * controller is called to refresh the UI as needed.
     * @param logEntry A new or updated {@link LogEntry}
     */
    public void handleLogEntryChange(LogEntry logEntry){
        logEntryTable.logEntryChanged(logEntry);
    }
}
