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

public class Preferences
{
	/** splash */
	public static final String SPLASH = "splash";
    /** default_apps */
    @Preference public static String[] default_apps;
    /** home_display */
    @Preference public static String home_display;
    /** top_resources */
    @Preference public static String top_resources;
    /** toolbar_entries */
    @Preference public static String toolbar_entries;
    /** splash */
    @Preference public static boolean splash;
    /** welcome */
    @Preference public static String welcome;
    /** max_array_formatting */
    @Preference public static int max_array_formatting;
    /** ui_monitor_period */
    @Preference public static int ui_monitor_period;
    /** hide_spi_menu */
    @Preference public static String[] hide_spi_menu;
    /** status_show_user */
    @Preference public static boolean status_show_user;
    /** default_save_path */
    @Preference public static String default_save_path;
    /** layout_dir */
    @Preference public static String layout_dir;
    /** layout_default absolute path*/
    @Preference public static String layout_default;
    /** layout_default absolute path*/
    @Preference public static boolean save_layout_in_layout_dir;
    /** print_landscape */
    @Preference public static boolean print_landscape;
    /** ok_severity_text_color */
    @Preference public static int[] ok_severity_text_color;
    /** minor_severity_text_color */
    @Preference public static int[] minor_severity_text_color;
    /** major_severity_text_color */
    @Preference public static int[] major_severity_text_color;
    /** invalid_severity_text_color */
    @Preference public static int[] invalid_severity_text_color;
    /** undefined_severity_text_color */
    @Preference public static int[] undefined_severity_text_color;
    /** ok_severity_background_color */
    @Preference public static int[] ok_severity_background_color;
    /** minor_severity_background_color */
    @Preference public static int[] minor_severity_background_color;
    /** major_severity_background_color */
    @Preference public static int[] major_severity_background_color;
    /** invalid_severity_background_color */
    @Preference public static int[] invalid_severity_background_color;
    /** undefined_severity_background_color */
    @Preference public static int[] undefined_severity_background_color;
    // Alarm Area Panel Configuration:
    /** ok_severity_text_color */
    @Preference public static int[] alarm_area_panel_ok_severity_text_color;
    /** minor_severity_text_color */
    @Preference public static int[] alarm_area_panel_minor_severity_text_color;
    /** major_severity_text_color */
    @Preference public static int[] alarm_area_panel_major_severity_text_color;
    /** invalid_severity_text_color */
    @Preference public static int[] alarm_area_panel_invalid_severity_text_color;
    /** undefined_severity_text_color */
    @Preference public static int[] alarm_area_panel_undefined_severity_text_color;
    /** ok_severity_background_color */
    @Preference public static int[] alarm_area_panel_ok_severity_background_color;
    /** minor_severity_background_color */
    @Preference public static int[] alarm_area_panel_minor_severity_background_color;
    /** major_severity_background_color */
    @Preference public static int[] alarm_area_panel_major_severity_background_color;
    /** invalid_severity_background_color */
    @Preference public static int[] alarm_area_panel_invalid_severity_background_color;
    /** undefined_severity_background_color */
    @Preference public static int[] alarm_area_panel_undefined_severity_background_color;
    /** cache_hint_for_picture_and_symbol_widgets */
    @Preference public static String cache_hint_for_picture_and_symbol_widgets;
    @Preference public static boolean save_credentials;

    static
    {
    	AnnotatedPreferences.initialize(Preferences.class, "/phoebus_ui_preferences.properties");

        // In case PVA library is included, sync its array formatting
        // (PVASettings cannot use Preferences.max_array_formatting
        //  since the PVA library may be used standalone)
        System.setProperty("EPICS_PVA_MAX_ARRAY_FORMATTING", Integer.toString(max_array_formatting));
    }
}
