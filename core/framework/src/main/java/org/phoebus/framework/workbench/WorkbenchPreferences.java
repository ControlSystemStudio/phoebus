/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
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

import org.phoebus.framework.preferences.PreferencesReader;

/** Workbench Preferences
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WorkbenchPreferences
{
    /** Logger for the 'workbench' package */
    public static final Logger logger = Logger.getLogger(WorkbenchPreferences.class.getPackageName());


    public static final Collection<String> external_apps;
    public static final File external_apps_directory;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(WorkbenchPreferences.class, "/workbench_preferences.properties");
        external_apps_directory = new File(prefs.get("external_apps_directory"));
        external_apps = prefs.getKeys("external_app_.*").stream().map(prefs::get).collect(Collectors.toList());
    }
}
