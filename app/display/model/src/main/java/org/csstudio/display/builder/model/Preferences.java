/*******************************************************************************
 * Copyright (c) 2015-2023 Oak Ridge National Laboratory.
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
import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Preference settings
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    /** Preference setting */
    @Preference public static String[] class_files, color_files, font_files;
    /** Preference setting */
    @Preference public static int read_timeout, cache_timeout, max_reparse_iterations;
    /** Preference setting */
    @Preference public static double legacy_font_calibration;
    /** Preference setting */
    @Preference public static boolean with_comments;
    /** Preference setting */
    @Preference public static boolean skip_defaults;
    /** Preference setting */
    @Preference(name="macros") private static String macro_spec;
    /** Preference setting */
    @Preference public static boolean enable_saved_on_comments;
    /** Preference setting */
    @Preference public static boolean enable_svg_rendering_resolution_factor;

    private static Macros macros;

    static
    {
    	AnnotatedPreferences.initialize(Preferences.class, "/display_model_preferences.properties");

        try
        {
            macros = MacroXMLUtil.readMacros(macro_spec);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error in macro preference", ex);
            macros = new Macros();
        }
    }

    /** @return Macros */
    public static Macros getMacros()
    {
        return new Macros(macros);
    }
}
