/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtable;

import org.phoebus.framework.preferences.PreferencesReader;

/** Preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Settings
{
    private static final String TREAT_BYTE_ARRAY_AS_STRING = "treat_byte_array_as_string";
    private static final String SHOW_UNITS = "show_units";
    private static final String SHOW_DESCRIPTION = "show_description";
    private static final String TOLERANCE = "tolerance";
    private static final String MAX_UPDATE_PERIOD = "max_update_period";

    public static boolean treat_byte_array_as_string;
    public static boolean show_units;
    public static boolean show_description;
    public static double tolerance;
    public static int update_item_threshold = 50;
    public static long max_update_period_ms = 500;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Settings.class, "/pv_table_preferences.properties");
        treat_byte_array_as_string = prefs.getBoolean(TREAT_BYTE_ARRAY_AS_STRING);
        show_units = prefs.getBoolean(SHOW_UNITS);
        show_description = prefs.getBoolean(SHOW_DESCRIPTION);
        tolerance = prefs.getDouble(TOLERANCE);
        max_update_period_ms = prefs.getInt(MAX_UPDATE_PERIOD);
    }
}
