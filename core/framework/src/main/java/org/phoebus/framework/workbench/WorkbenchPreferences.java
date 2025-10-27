/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.workbench;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Workbench Preferences
 *  @author Kay Kasemir
 */
public class WorkbenchPreferences
{
    // This class gets initialized quite early because other code uses this logger.
    // The external apps are thus read on demand via getExternalApps(),
    // they are not constructed in the static... code below because
    // that might happen before all the `-settings` from the command line have been applied.

    /** Logger for the 'workbench' package */
    public static final Logger logger = Logger.getLogger(WorkbenchPreferences.class.getPackageName());

    /** directory of external applications */
    @Preference public static File external_apps_directory;

    /** Phoebus memento folder name default from $(phoebus.folder.name.preference) System property */
    @Preference public static String  phoebus_folder_name;

    /** Phoebus user home directory contents memento default from $(phoebus.user) System property */
    @Preference public static File  phoebus_user;

    /** @return external application definitions */
    public static Collection<String> getExternalApps()
    {
        Preferences prefs = Preferences.userNodeForPackage(WorkbenchPreferences.class);
        final List<String> external = new ArrayList<>();
        try
        {
            for (String key : prefs.keys())
                if (key.startsWith("external_app_"))
                    external.add(prefs.get(key, ""));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot read external_app_* entries", ex);
        }
        return external;
    }

    static
    {
        AnnotatedPreferences.initialize(WorkbenchPreferences.class, "/workbench_preferences.properties");
    }
}
