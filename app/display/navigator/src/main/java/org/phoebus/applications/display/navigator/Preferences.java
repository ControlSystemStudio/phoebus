package org.phoebus.applications.display.navigator;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
public class Preferences
{
    static {
        AnnotatedPreferences.initialize(Preferences.class, "/navigator_preferences.properties");
    }

    @Preference
    public static String navigator_root;
    @Preference
    public static String initial_navigator;
    @Preference
    public static String opi_root;
}
