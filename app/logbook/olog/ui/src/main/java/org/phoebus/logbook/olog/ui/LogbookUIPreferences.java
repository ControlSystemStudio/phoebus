/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.olog.ui;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.logbook.LogService;

import java.util.logging.Level;

import static org.phoebus.ui.application.PhoebusApplication.logger;

/** Preference settings for logbook.ui
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class LogbookUIPreferences
{
    @Preference public static String[] default_logbooks;
    @Preference public static String default_logbook_query;
    @Preference public static boolean save_credentials;
    @Preference public static String calendar_view_item_stylesheet;
    @Preference public static String level_field_name;
    @Preference public static String markup_help;
    @Preference public static String web_client_root_URL;
    @Preference public static boolean log_entry_groups_support;
    @Preference public static String[] hidden_properties;
    @Preference public static String[] auto_properties;

    static
    {
        final PreferencesReader prefs = AnnotatedPreferences.initialize(LogbookUIPreferences.class, "/log_olog_ui_preferences.properties");
    }
}
