/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtable;

import java.util.prefs.Preferences;

/** Preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Settings
{
    private static final String TREAT_BYTE_ARRAY_AS_STRING = "treat_byte_array_as_string";
    private static final String SHOW_UNITS = "show_units";
    private static final String SHOW_DESCRIPTION = "show_description";

    public static final boolean treat_byte_array_as_string = treatByteArrayAsString();

    public static boolean show_units = showUnits();

    public static boolean show_description = showDescription();
    public static double tolerance = 0.1;
    public static int update_item_threshold = 50;

    private static boolean treatByteArrayAsString()
    {
        final Preferences prefs = Preferences.userNodeForPackage(Settings.class);
        boolean value = prefs.getBoolean(TREAT_BYTE_ARRAY_AS_STRING, true);
        prefs.putBoolean(TREAT_BYTE_ARRAY_AS_STRING, value);
        return value;
    }

    private static boolean showUnits()
    {
        final Preferences prefs = Preferences.userNodeForPackage(Settings.class);
        boolean value = prefs.getBoolean(SHOW_UNITS, true);
        prefs.putBoolean(SHOW_UNITS, value);
        return value;
    }

    private static boolean showDescription()
    {
        final Preferences prefs = Preferences.userNodeForPackage(Settings.class);
        boolean value = prefs.getBoolean(SHOW_DESCRIPTION, true);
        prefs.putBoolean(SHOW_DESCRIPTION, value);
        return value;
    }

}
