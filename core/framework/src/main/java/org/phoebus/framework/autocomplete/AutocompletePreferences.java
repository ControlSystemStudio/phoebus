/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.preferences.PreferencesReader;

/** Autocomplete Preferences
 *  @author Tynan Ford
 */
@SuppressWarnings("nls")
public class AutocompletePreferences
{
    /** Logger for the 'autocomplete' package */
    public static final Logger logger = Logger.getLogger(AutocompletePreferences.class.getPackageName());

    @Preference public static boolean enable_loc_pv_proposals;
    @Preference public static boolean enable_sim_pv_proposals;
    @Preference public static boolean enable_sys_pv_proposals;
    @Preference public static boolean enable_pva_pv_proposals;
    @Preference public static boolean enable_mqtt_pv_proposals;
    @Preference public static boolean enable_formula_proposals;

    static
    {
        final PreferencesReader prefs = AnnotatedPreferences.initialize(AutocompletePreferences.class, "/autocomplete_preferences.properties");
    }
}
