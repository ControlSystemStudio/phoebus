/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.channelfinder;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/**
 * The {@link Preferences} object holds the properties associated with the
 * channel finder client library initialized using the channelfinder_preferences.properties
 * or default values.
 *
 * The order in which these files will be read. 1. properties file specified
 * using the system property <tt>channelfinder_preferences.properties</tt>. 2.
 * channelfinder_preferences.properties file in the users home direcotory. 3.
 * channelfinder_preferences.properties file in the C:/ on windows and /etc/ on linux. 4.
 * channelfinder_preferences.properties default file packaged with the library.
 *
 * @author shroffk
 *
 */
public class Preferences {

    @Preference
    public static String serviceURL;

    @Preference
    public static boolean rawFiltering;

    @Preference
    public static String username;

    @Preference
    public static String password;

    static {
        AnnotatedPreferences.initialize(Preferences.class, "/channelfinder_preferences.properties");
    }
}
