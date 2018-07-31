package org.phoebus.logbook.ui;

import java.net.URI;
import java.util.logging.Logger;

import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogFactory;
import org.phoebus.logbook.LogService;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;

public class LogEntryListApp implements AppResourceDescriptor {

    public static final Logger logger = Logger.getLogger(LogEntryListApp.class.getName());
    static final Image icon = ImageCache.getImage(LogEntryListApp.class, "/icons/logbook-16.png");
    public static final String NAME = "logEntryList";
    public static final String DISPLAYNAME = "Log Entry List";

    private static final String SUPPORTED_SCHEMA = "logbook";
    private LogFactory logFactory;

    @Override
    public void start() {
        logFactory = LogService.getInstance().getLogFactories().get(LogbookUiPreferences.logbook_factory);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AppInstance create() {
        return new LogEntryList(this);
    }

    @Override
    public boolean canOpenResource(String resource) {
        return URI.create(resource).getScheme().equals(SUPPORTED_SCHEMA);
    }

    /**
     * Support the launching of log entry list view using resource
     * logbook://?<search_string> e.g. -resource
     * logbook://?query=*Fault*Motor*&tag=operation
     */
    @Override
    public AppInstance create(URI resource) {
        LogEntryList logEntryTable = new LogEntryList(this);
        logEntryTable.setResource(resource);
        return logEntryTable;
    }

    public LogClient getClient() {
        return logFactory.getLogClient();
    }
}
