/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.queueserver;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

public class Preferences {

    @Preference
    public static String queue_server_url;

    @Preference
    public static String api_key;

    @Preference
    public static boolean debug;

    @Preference
    public static int connectTimeout;

    static
    {
        AnnotatedPreferences.initialize(Preferences.class, "/queueserver_preferences.properties");
    }
}