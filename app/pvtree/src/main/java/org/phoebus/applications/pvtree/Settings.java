/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtree;

import static org.phoebus.applications.pvtree.PVTreeApplication.logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.preferences.PreferencesReader;

/** Preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Settings
{
    @Preference public static boolean read_long_fields;
    @Preference(name="update_period") public static double max_update_period;
    public static Map<String, List<String>> field_info;

    static
    {
        final PreferencesReader prefs = AnnotatedPreferences.initialize(Settings.class, "/pv_tree_preferences.properties");

        final String spec = prefs.get("fields");
        try
        {
            field_info = FieldParser.parse(spec);
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot parse fields from '" + spec + "'", ex);
            field_info = Collections.emptyMap();
        }
    }
}
