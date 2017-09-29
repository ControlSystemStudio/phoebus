/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.preferences;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;


/** Reads preferences while using property file for defaults
 *
 *  <p>We use property files to define the defaults
 *  for preference settings, in part because a property file
 *  allows for comments that describe the meaning and possible
 *  values for each settings.
 *
 *  <p>The {@link Preferences} API is otherwise used to
 *  read user settings.
 *
 *  <p>This helper assists with reading the defaults
 *  from a property file, while then allowing user
 *  settings to be added from the {@link Preferences}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PreferencesReader
{
    private final Properties defaults = new Properties();
    private final Preferences prefs;

    /** Create reader for preferences
     *
     *  @param package_class Class of the package.
     *                       This class provides the package path that is used
     *                       for the preference node,
     *                       and its class loader is used to read the property file.
     *  @param preferences_properties_filename Name of the property file
     */
    public PreferencesReader(Class<?> package_class, String preferences_properties_filename)
    {
        try
        {
            defaults.load(package_class.getResourceAsStream(preferences_properties_filename));
        }
        catch (Exception ex)
        {
            Logger.getLogger(getClass().getName())
                  .log(Level.SEVERE, "Cannot read default preference settings for " + package_class + " from " + preferences_properties_filename);
        }
        prefs = Preferences.userNodeForPackage(package_class);
    }

    /** @param key Key for preference setting
     *  @return String value from preferences, defaulting to value from property file
     */
    public String get(final String key)
    {
        return prefs.get(key, defaults.getProperty(key));
    }

    /** @param key Key for preference setting
     *  @return Boolean value from preferences, defaulting to value from property file
     */
    public boolean getBoolean(final String key)
    {
        return prefs.getBoolean(key, Boolean.parseBoolean(defaults.getProperty(key)));
    }

    /** @param key Key for preference setting
     *  @return Int value from preferences, defaulting to value from property file
     */
    public int getInt(final String key)
    {
        return prefs.getInt(key, Integer.parseInt(defaults.getProperty(key)));
    }

    /** @param key Key for preference setting
     *  @return Double value from preferences, defaulting to value from property file
     */
    public double getDouble(final String key)
    {
        return prefs.getDouble(key, Double.parseDouble(defaults.getProperty(key)));
    }
}
