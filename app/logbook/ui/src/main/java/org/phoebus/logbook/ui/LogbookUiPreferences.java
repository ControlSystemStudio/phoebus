/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui;

import org.phoebus.framework.preferences.PreferencesReader;

/** Preference settings for logbook.ui
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class LogbookUiPreferences
{
    public static final String[] default_logbooks;
    public static final boolean save_credentials;
    public static final String   logbook_factory;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(LogbookUiPreferences.class, "/log_ui_preferences.properties");

        // Split the comma separated list.
        default_logbooks = prefs.get("default_logbooks").split("(\\s)*,(\\s)*");
        save_credentials = prefs.getBoolean("save_credentials");
        logbook_factory  = prefs.get("logbook_factory");
    }
}
