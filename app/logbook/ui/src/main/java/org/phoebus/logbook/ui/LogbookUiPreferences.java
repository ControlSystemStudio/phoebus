/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.util.logging.Level;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.logbook.LogService;

/** Preference settings for logbook.ui
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class LogbookUiPreferences
{
    @Preference public static String[] default_logbooks;
    @Preference public static String default_logbook_query;
    @Preference public static boolean  save_credentials;
    @Preference public static String calendar_view_item_stylesheet;
    @Preference public static String level_field_name;

    static
    {
        final PreferencesReader prefs = AnnotatedPreferences.initialize(LogbookUiPreferences.class, "/log_ui_preferences.properties");
    }
}
