/*******************************************************************************
 * Copyright (c) 2017-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Preference settings
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static final String SPLASH = "splash";

    @Preference public static String[] default_apps;
    @Preference public static String home_display;
    @Preference public static String top_resources;
    @Preference public static boolean splash;
    @Preference public static String welcome;
    @Preference public static int max_array_formatting;
    @Preference public static int ui_monitor_period;
    @Preference public static String[] hide_spi_menu;
    @Preference public static boolean status_show_user;
    @Preference public static String default_save_path;
    @Preference public static String layout_dir;
    @Preference public static boolean print_landscape;
    @Preference public static int[] ok_severity_text_color;
    @Preference public static int[] minor_severity_text_color;
    @Preference public static int[] major_severity_text_color;
    @Preference public static int[] invalid_severity_text_color;
    @Preference public static int[] undefined_severity_text_color;
    @Preference public static int[] ok_severity_background_color;
    @Preference public static int[] minor_severity_background_color;
    @Preference public static int[] major_severity_background_color;
    @Preference public static int[] invalid_severity_background_color;
    @Preference public static int[] undefined_severity_background_color;

    static
    {
    	AnnotatedPreferences.initialize(Preferences.class, "/phoebus_ui_preferences.properties");

        // In case PVA library is included, sync its array formatting
        // (PVASettings cannot use Preferences.max_array_formatting
        //  since the PVA library may be used standalone)
        System.setProperty("EPICS_PVA_MAX_ARRAY_FORMATTING", Integer.toString(max_array_formatting));
    }
}
