/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.ui.application.PhoebusApplication;

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
    public static final String authorization_file;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "/phoebus_ui_preferences.properties");
        default_apps = prefs.get(DEFAULT_APPS).split("\\s*,\\s*");
        top_resources = prefs.get(TOP_RESOURCES);
        splash = prefs.getBoolean(SPLASH);
        authorization_file = replaceProperties(prefs.get("authorization_file"));
    }

    /** @param value Value that might contain "$(prop)"
     *  @return Value where "$(prop)" is replaced by Java system property "prop"
     */
    private static String replaceProperties(final String value)
    {
        final Matcher matcher = Pattern.compile("\\$\\((.*)\\)").matcher(value);
        if (matcher.matches())
        {
            final String prop_name = matcher.group(1);
            final String prop = System.getProperty(prop_name);
            if (prop == null)
                PhoebusApplication.logger.log(Level.SEVERE, "UI Preferences: Property '" + prop_name + "' is not defined");
            else
                return prop;
        }
        // Return as is
        return value;
    }
}
