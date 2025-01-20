/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.olog.api;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

public class Preferences {

    @Preference
    public static String olog_url;

    @Preference
    public static String username;

    @Preference
    public static String password;

    @Preference
    public static boolean debug;

    @Preference
    public static int connectTimeout;

    static
    {
        AnnotatedPreferences.initialize(Preferences.class, "/olog_preferences.properties");
    }
}
