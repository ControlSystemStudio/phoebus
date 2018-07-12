/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
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
    public static final String TOP_RESOURCES = "top_resources";
    public static final String SPLASH = "splash";

    public static String[] default_apps;
    public static String top_resources;
    public static boolean splash;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "/phoebus_ui_preferences.properties");
        default_apps = prefs.get(DEFAULT_APPS).split("\\s*,\\s*");
        top_resources = prefs.get(TOP_RESOURCES);
        splash = prefs.getBoolean(SPLASH);
    }
}
