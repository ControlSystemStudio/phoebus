/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.PatternSyntaxException;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.preferences.PreferencesReader;

/** Preference settings
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    /** Preference setting */
    @Preference public static String python_path;
    /** Preference setting */
    @Preference(name="update_throttle") public static int update_throttle_ms;
    /** Preference setting */
    @Preference public static String probe_display;
    /** Preference setting */
    public static final List<TextPatch> pv_name_patches = new ArrayList<>();
    /** Preference setting */
    @Preference public static int default_zoom_factor;

    static
    {
    	final PreferencesReader prefs = AnnotatedPreferences.initialize(Preferences.class, "/display_runtime_preferences.properties");

    	final String setting = prefs.get("pv_name_patches");
    	if (! setting.isEmpty())
        {
            // Split on '@', except if preceded by '[' to skip '[@]'
            final String[] config = setting.split("(?<!\\[)@");
            if (config.length % 2 == 0)
            {
                for (int i=0; i<config.length; i+=2)
                {
                    final TextPatch patch;
                    try
                    {
                        patch = new TextPatch(config[i], config[i+1]);
                    }
                    catch (PatternSyntaxException ex)
                    {
                        logger.log(Level.SEVERE, "Error in PV name patch '" + config[i] + "' -> '" + config[i+1] + "'", ex);
                        continue;
                    }
                    pv_name_patches.add(patch);
                    logger.log(Level.CONFIG, patch.toString());
                }
            }
            else
                logger.log(Level.SEVERE, "Invalid setting for pv_name_patches," +
                                         "need even number of items (pairs of pattern@replacement)");
        }
    }
}
