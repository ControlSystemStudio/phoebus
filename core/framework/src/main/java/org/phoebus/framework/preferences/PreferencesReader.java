/*******************************************************************************
 * Copyright (c) 2017-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.preferences;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
    private final String package_class;
    private final Preferences prefs;

    private static final Pattern PROP_PATTERN = Pattern.compile("\\$\\([^\\)]+\\)");

    /** Replace one or more property references
     *  @param value Value that might contain "$(prop)"
     *  @return Value where "$(prop)" is replaced by Java system property "prop"
     */
    static String replaceProperties(final String value)
    {
        if (value == null)
            return value;
        String result = value;
        Matcher matcher = PROP_PATTERN.matcher(value);
        while (matcher.find())
        {
            final String prop_spec = matcher.group();
            final String prop_name = prop_spec.substring(2, prop_spec.length()-1);
            final int start = matcher.start();
            final int end = matcher.end();
            String prop = System.getProperty(prop_name);
            if (prop == null)
                prop = System.getenv(prop_name);
            if (prop == null)
            {
                Logger.getLogger(PreferencesReader.class.getPackageName())
                      .log(Level.SEVERE, "Reading Preferences: Java system property or Environment variable'" + prop_spec + "' is not defined");
                break;
            }
            else
                result = result.substring(0, start) + prop + result.substring(end);
            matcher = PROP_PATTERN.matcher(result);
        }
        return result;
    }

    /** Create reader for preferences
     *
     *  @param package_class Class of the package.
     *                       This class provides the package path that is used
     *                       for the preference node,
     *                       and its class loader is used to read the property file.
     *  @param preferences_properties_filename Name of the property file
     */
    public PreferencesReader(final Class<?> package_class, final String preferences_properties_filename)
    {
        this.package_class = package_class.getPackageName();
        try
        {
            defaults.load(package_class.getResourceAsStream(preferences_properties_filename));
        }
        catch (Exception ex)
        {
            Logger.getLogger(PreferencesReader.class.getPackageName())
                  .log(Level.SEVERE, "Cannot read default preference settings for " + package_class + " from " + preferences_properties_filename);
        }
        prefs = Preferences.userNodeForPackage(package_class);
    }

    /** Get all preference keys that match a pattern
     *  @param regex Regular expression
     *  @return Keys that have a preference setting
     */
    public Set<String> getKeys(final String regex)
    {
        final Set<String> keys = new HashSet<>();
        final Pattern pattern = Pattern.compile(regex);
        try
        {
            // Check keys in default settings
            for (Object o : defaults.keySet())
            {
                final String key = o.toString();
                if (pattern.matcher(key).matches())
                    keys.add(key);
            }
            // Check keys in updated preferences
            for (String key : prefs.keys())
                if (pattern.matcher(key).matches())
                    keys.add(key);
        }
        catch (BackingStoreException ex)
        {
            Logger.getLogger(PreferencesReader.class.getPackageName())
                  .log(Level.SEVERE, "Cannot locate preference entries matching '" + regex + "'", ex);
        }
        return keys;
    }

    /** @param key Key for preference setting
     *  @return String value from preferences, defaulting to value from property file
     */
    public String get(final String key)
    {
        final String value = prefs.get(key, defaults.getProperty(key));
        if (value == null)
        {
            Logger.getLogger(PreferencesReader.class.getPackageName())
                  .log(Level.SEVERE, "No default setting for preference " + package_class + "/" + key);
            return "";
        }
        return replaceProperties(value);
    }

    /** @param key Key for preference setting
     *  @return Boolean value from preferences, defaulting to value from property file
     */
    public boolean getBoolean(final String key)
    {
        return Boolean.parseBoolean(get(key));
    }

    /** @param key Key for preference setting
     *  @return Int value from preferences, defaulting to value from property file
     */
    public int getInt(final String key)
    {
        final String value = get(key);
        if (value.isBlank())
            return 0;
        return Integer.parseInt(value);
    }

    /** @param key Key for preference setting
     *  @return Double value from preferences, defaulting to value from property file
     */
    public double getDouble(final String key)
    {
        final String value = get(key);
        if (value.isBlank())
            return 0.0;
        return Double.parseDouble(value);
    }
}
