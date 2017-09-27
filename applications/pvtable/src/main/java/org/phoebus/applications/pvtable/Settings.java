/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtable;

import java.util.Properties;
import java.util.logging.Level;
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
    private static final String TOLERANCE = "tolerance";

    public static boolean treat_byte_array_as_string;
    public static boolean show_units;
    public static boolean show_description;
    public static double tolerance;
    public static int update_item_threshold = 50;

    static
    {
        try
        {
            final Properties defaults = new Properties();
            defaults.load(Settings.class.getResourceAsStream("/preferences.properties"));

            final Preferences prefs = Preferences.userNodeForPackage(Settings.class);

            treat_byte_array_as_string = Boolean.parseBoolean(prefs.get(TREAT_BYTE_ARRAY_AS_STRING, defaults.getProperty(TREAT_BYTE_ARRAY_AS_STRING)));
            show_units = Boolean.parseBoolean(prefs.get(SHOW_UNITS, defaults.getProperty(SHOW_UNITS)));
            show_description = Boolean.parseBoolean(prefs.get(SHOW_DESCRIPTION, defaults.getProperty(SHOW_DESCRIPTION)));
            tolerance = Double.parseDouble(prefs.get(TOLERANCE, defaults.getProperty(TOLERANCE)));

        }
        catch (Exception ex)
        {
            PVTableApplication.logger.log(Level.SEVERE, "Cannot get preferences", ex);
        }
    }
}
