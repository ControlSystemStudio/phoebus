/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.phoebus.framework.preferences.PreferencesReader;

/** Phoebus application for EDM converter
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ConverterPreferences
{
    public static String colors_list;

    public static final List<String> paths = new ArrayList<>();

    public static volatile File auto_converter_dir;

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

        final String edm_paths_config = prefs.get("edm_paths_config").trim();
        if (! edm_paths_config.isEmpty())
            try
            {
                parseEdmPaths(edm_paths_config);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot parse paths from " + edm_paths_config, ex);
            }

        final String dir = prefs.get("auto_converter_dir");
        if (dir.isBlank())
            auto_converter_dir = null;
        else
        {
            final File folder = new File(dir);
            if (folder.exists() && folder.isDirectory())
                auto_converter_dir = folder;
            else
            {
                auto_converter_dir = null;
                logger.log(Level.WARNING, "EDM auto_converter_dir " + dir + " does not exist");
            }
        }
    }

    public static void parseEdmPaths(final String edm_paths_config) throws Exception
    {
        paths.clear();
        try
        (
            final BufferedReader reader = new BufferedReader(new InputStreamReader(ModelResourceUtil.openResourceStream(edm_paths_config)));
        )
        {
            String line;
            while ((line = reader.readLine()) != null)
                if (! line.startsWith("#"))
                    paths.add(line);
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

    public static void setAutoConverterDir(final String path)
    {
        auto_converter_dir = new File(path);

        final Preferences prefs = Preferences.userNodeForPackage(ConverterPreferences.class);
        prefs.put("auto_converter_dir",  path);
        try
        {
            prefs.flush();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot update auto_converter_dir", ex);
        }
    }
}
