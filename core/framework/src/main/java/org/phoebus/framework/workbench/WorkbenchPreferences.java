/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.workbench;

import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.preferences.PreferencesReader;

/** Workbench Preferences
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WorkbenchPreferences
{
    /** Logger for the 'workbench' package */
    public static final Logger logger = Logger.getLogger(WorkbenchPreferences.class.getPackageName());

    @Preference public static File external_apps_directory;
    public static final Collection<String> external_apps;

    static
    {
        final PreferencesReader prefs = AnnotatedPreferences.initialize(WorkbenchPreferences.class, "/workbench_preferences.properties");
        external_apps = prefs.getKeys("external_app_.*").stream().map(prefs::get).collect(Collectors.toList());
    }
}
