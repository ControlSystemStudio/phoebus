/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.phoebus.framework.preferences.PreferencesReader;

/** Phoebus application for EDM converter
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ConverterPreferences
{
    public static String colors_list;

    private static class FontMapping
    {
        final Pattern pattern;
        final String font_name;

        public FontMapping(final String pattern, final String font_name)
        {
            this.pattern = Pattern.compile(pattern);
            this.font_name = font_name;
        }
    }
    private static List<FontMapping> font_mappings = new ArrayList<>();

    static
    {
        final PreferencesReader prefs = new PreferencesReader(ConverterPreferences.class, "/edm_converter_preferences.properties");
        colors_list = prefs.get("colors_list");

        try
        {
            parseFontMappings(prefs.get("font_mappings"));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot parse font_mappings", ex);
        }
    }

    private static void parseFontMappings(final String pref) throws Exception
    {
        for (String map : pref.split("\\s*,\\s*"))
        {
            String[] pattern_font = map.split("\\s*=\\s*");
            if (pattern_font.length != 2)
                throw new Exception("Invalid font mapping '" + map + "'");
            font_mappings.add(new FontMapping(pattern_font[0], pattern_font[1]));
        }
    }

    public static String mapFont(final String edm_font_name)
    {
        for (FontMapping mapping : font_mappings)
            if (mapping.pattern.matcher(edm_font_name).matches())
            {
                logger.log(Level.FINE, "Mapping EDM font '" + edm_font_name + "' to '" + mapping.font_name + "'");
                return mapping.font_name;
            }
        return edm_font_name;
    }
}
