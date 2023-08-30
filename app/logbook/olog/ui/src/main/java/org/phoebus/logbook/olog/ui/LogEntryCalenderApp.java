package org.phoebus.logbook.olog.ui;

import com.google.common.base.Strings;
import javafx.scene.image.Image;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogFactory;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.ui.javafx.ImageCache;

import java.net.URI;
import java.util.logging.Logger;

public class LogEntryCalenderApp implements AppResourceDescriptor {

    public static final Logger logger = Logger.getLogger(LogEntryCalenderApp.class.getName());
    static final Image icon = ImageCache.getImage(LogEntryCalenderApp.class, "/icons/logbook-16.png");
    public static final String NAME = "logEntryCalender";
    public static String DISPLAYNAME = "Log Entry Calender";

    private static final String SUPPORTED_SCHEMA = "logCalender";
    private LogFactory logFactory;

    @Override
    public void start() {
        logFactory = LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory);
        String displayName = LogbookUIPreferences.log_entry_calendar_display_name;
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
        return new LogEntryCalender(this);
    }

    @Override
    public boolean canOpenResource(String resource) {
        return URI.create(resource).getScheme().equals(SUPPORTED_SCHEMA);
    }

    /**
     * Support the launching of log entry table using resource
     * logbook://?<search_string> e.g. -resource
     * logbook://?search=*Fault*Motor*&tag=operation
     */
    @Override
    public AppInstance create(URI resource) {
        LogEntryCalender logEntryCalender = new LogEntryCalender(this);
        return logEntryCalender;
    }

    public LogClient getClient() {
        return logFactory.getLogClient();
    }
}
