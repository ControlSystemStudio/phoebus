/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.scan.ui;


import java.util.prefs.Preferences;

import org.phoebus.framework.preferences.PreferencesReader;

/** Preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanUIPreferences
{
    public static boolean monitor_status;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(ScanUIPreferences.class, "/scan_ui_preferences.properties");
        monitor_status = prefs.getBoolean("monitor_status");
    }

    /** @param show New 'monitor_status' preference value to write */
    public static void setMonitorStatus(final boolean show)
    {
        Preferences prefs = Preferences.userNodeForPackage(ScanUIPreferences.class);
        prefs.putBoolean("monitor_status", show);
    }
}
