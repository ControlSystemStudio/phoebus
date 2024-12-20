/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.olog.es.api;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

public class Preferences {

    @Preference
    public static String olog_url;

    @Preference
    public static int connectTimeout;

    @Preference
    public static boolean permissive_hostname_verifier;

    @Preference public static String[] levels;

    static
    {
        AnnotatedPreferences.initialize(Preferences.class, "/olog_es_preferences.properties");
    }
}
