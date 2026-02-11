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
    public static String api_key_file;

    @Preference
    public static boolean debug;

    @Preference
    public static int connectTimeout;

    @Preference
    public static boolean use_websockets;

    @Preference
    public static int update_interval_ms;

    static
    {
        AnnotatedPreferences.initialize(Preferences.class, "/queueserver_preferences.properties");
    }
}