/*******************************************************************************
 * Copyright (c) 2017-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui;

import org.phoebus.framework.preferences.PreferencesReader;

/** Preference settings
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static final String DEFAULT_APPS = "default_apps";
    public static final String HOME_DISPLAY = "home_display";
    public static final String TOP_RESOURCES = "top_resources";
    public static final String SPLASH = "splash";

    public static final String[] default_apps;
    public static final String home_display;
    public static final String top_resources;
    public static final boolean splash;
    public static final String welcome;
    public static final int ui_monitor_period;
    public static final String[] hide_spi_menu;
    public static final boolean status_show_user;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "/phoebus_ui_preferences.properties");
        default_apps = prefs.get(DEFAULT_APPS).split("\\s*,\\s*");
        home_display = prefs.get(HOME_DISPLAY);
        top_resources = prefs.get(TOP_RESOURCES);
        splash = prefs.getBoolean(SPLASH);
        welcome = prefs.get("welcome");
        ui_monitor_period = prefs.getInt("ui_monitor_period");
        hide_spi_menu = prefs.get("hide_spi_menu").split("\\s*,\\s*");
        status_show_user = prefs.getBoolean("status_show_user");
    }
}
