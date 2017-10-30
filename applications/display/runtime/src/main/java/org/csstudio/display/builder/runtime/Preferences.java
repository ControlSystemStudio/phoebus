/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
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

import org.phoebus.framework.preferences.PreferencesReader;

/** Preference settings
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static final String PYTHON_PATH = "python_path";
    public static final String PV_NAME_PATCHES = "pv_name_patches";

    public static String python_path;
    public static List<TextPatch> pv_name_patches;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "display_runtime_preferences.properties");
        python_path = prefs.get(PYTHON_PATH);

        pv_name_patches = new ArrayList<>();
        final String setting = prefs.get(PV_NAME_PATCHES);
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
                    logger.config(patch.toString());
                }
            }
            else
                logger.log(Level.SEVERE, "Invalid setting for " + PV_NAME_PATCHES +
                                         ", need even number of items (pairs of pattern@replacement)");
        }
    }
}
