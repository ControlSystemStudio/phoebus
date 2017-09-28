/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
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

import org.phoebus.framework.preferences.PreferencesReader;
/** Preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Settings
{
    private static final String UPDATE_PERIOD = "update_period";
    private static final String FIELDS = "fields";
    private static final String READ_LONG_FIELDS = "read_long_fields";

    public static final boolean read_long_fields;
    public static Map<String, List<String>> field_info;
    public static final double max_update_period;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Settings.class, "/pv_tree_preferences.properties");
        read_long_fields = prefs.getBoolean(READ_LONG_FIELDS);

        final String spec = prefs.get(FIELDS);
        try
        {
            field_info = FieldParser.parse(spec);
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot parse fields from '" + spec + "'", ex);
            field_info = Collections.emptyMap();
        }

        max_update_period = prefs.getDouble(UPDATE_PERIOD);
    }
}
