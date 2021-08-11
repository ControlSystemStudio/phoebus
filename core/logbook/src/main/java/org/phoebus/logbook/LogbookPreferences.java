package org.phoebus.logbook;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.preferences.PreferencesReader;

import java.util.logging.Level;

import static org.phoebus.logbook.LogService.logger;

public class LogbookPreferences {

    @Preference
    public static String logbook_factory;

    @Preference
    public static boolean auto_title;

    /** Is there support for a logbook?
     *  Is the 'logbook_factory' configured and available?
     */
    public static boolean is_supported;

    static
    {
        final PreferencesReader prefs = AnnotatedPreferences.initialize(LogbookPreferences.class, "/logbook_preferences.properties");
        if (logbook_factory.isEmpty())
        {
            is_supported = false;
            logger.log(Level.INFO, "No logbook factory selected");
        }
        else
        {
            is_supported = LogService.getInstance().getLogFactories(logbook_factory) != null;
            if (! is_supported)
                logger.log(Level.WARNING, "Cannot locate logbook factory '" + logbook_factory + "'");
        }
    }
}
