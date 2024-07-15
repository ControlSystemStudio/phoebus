package org.phoebus.pv.archive;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.preferences.PreferencesReader;

/** Helper for reading preference settings
 *
 *  @author Kunal Shroff
 */
@SuppressWarnings("nls")
public class Preferences
{

    /** Setting */
    @Preference public static String archive_url;

    static
    {
        final PreferencesReader prefs = AnnotatedPreferences.initialize(Preferences.class, "/appliance_datasource_preferences.properties");
    }

}
