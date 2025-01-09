/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.alarm.logging.ui;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

public class Preferences {

    @Preference
    public static String service_uri;

    @Preference
    public static int results_max_size;

    static
    {
        AnnotatedPreferences.initialize(Preferences.class, "/alarm_logging_preferences.properties");
    }
}
