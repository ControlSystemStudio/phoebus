/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.macros.MacroXMLUtil;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.preferences.PreferencesReader;

/** Preference settings
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static final String CACHE_TIMEOUT = "cache_timeout";
    public static final String CLASS_FILES = "class_files";
    public static final String COLOR_FILES = "color_files";
    public static final String FONT_FILES = "font_files";
    public static final String READ_TIMEOUT = "read_timeout";
    public static final String LEGACY_FONT_CALIBRATION = "legacy_font_calibration";
    public static final String MACROS = "macros";
    public static final String MAX_REPARSE_ITERATIONS = "max_reparse_iterations";
    public static final String SKIP_DEFAULTS = "skip_defaults";

    public static String[] class_files, color_files, font_files;
    public static int read_timeout, cache_timeout, max_reparse;
    public static double legacy_font_calibration;
    public static boolean skip_defaults;
    private static Macros macros;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "/display_model_preferences.properties");

        class_files = getFiles(prefs, CLASS_FILES);
        color_files = getFiles(prefs, COLOR_FILES);
        font_files = getFiles(prefs, FONT_FILES);
        read_timeout = prefs.getInt(READ_TIMEOUT);
        cache_timeout = prefs.getInt(CACHE_TIMEOUT);
        max_reparse = prefs.getInt(MAX_REPARSE_ITERATIONS);
        legacy_font_calibration = prefs.getDouble(LEGACY_FONT_CALIBRATION);
        skip_defaults = prefs.getBoolean(SKIP_DEFAULTS);
        try
        {
            macros = MacroXMLUtil.readMacros(prefs.get(MACROS));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error in macro preference", ex);
            macros = new Macros();
        }
    }

    private static String[] getFiles(final PreferencesReader prefs, final String key)
    {
        final String[] setting = prefs.get(key).split(" *; *");
        // split() will turn "" into { "" }, which we change into empty array
        if (setting.length == 0  ||  (setting.length == 1 && setting[0].isEmpty()))
            return new String[0];
        return setting;
    }

    public static Macros getMacros()
    {
        return new Macros(macros);
    }
}
