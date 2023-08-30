/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtable;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Settings
{
    /** Preference setting */
    @Preference public static boolean treat_byte_array_as_string;
    /** Preference setting */
    @Preference public static boolean show_units;
    /** Preference setting */
    @Preference public static boolean show_description;
    /** Preference setting */
    @Preference public static double tolerance;
    /** Preference setting */
    public static int update_item_threshold = 50;
    /** Preference setting */
    @Preference(name="max_update_period") public static long max_update_period_ms;

    static
    {
    	AnnotatedPreferences.initialize(Settings.class, "/pv_table_preferences.properties");
    }
}
